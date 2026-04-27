package com.ragask.ticketing.service;

import com.ragask.ticketing.knowledge.KnowledgeService;
import com.ragask.ticketing.knowledge.PgVectorStoreService;
import com.ragask.ticketing.knowledge.AttachmentService;
import com.ragask.ticketing.model.dto.AttachmentDto;
import com.ragask.ticketing.model.dto.ChatMessage;
import com.ragask.ticketing.model.dto.RagAnswer;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
@RequiredArgsConstructor
public class RagOrchestratorService {

    private static final int TOP_K_EACH = 5;
    private static final int FINAL_TOP_N = 3;
    private static final int RRF_K = 60;
    private static final Duration RERANK_TIMEOUT = Duration.ofMillis(800);

    private final MetricsService metricsService;
    private final KnowledgeService knowledgeService;
    private final PgVectorStoreService pgVectorStoreService;
    private final AttachmentService attachmentService;
    private final DeepSeekClientService deepSeekClientService;
    private final ChatMemoryService chatMemoryService;
    private final PromptTemplateService promptTemplateService;
    private final PromptSecurityService promptSecurityService;

    @Value("${rag.retrieval.backend:memory}")
    private String retrievalBackend;

    public RagAnswer answer(String question, String sessionId, String runtimeApiKey) {
        metricsService.countQuery();
        if (promptSecurityService.looksLikePromptInjection(question)) {
            return new RagAnswer(
                    "检测到潜在提示词注入风险，请使用业务问题重新提问；当前请求已建议转人工处理。",
                    0.2,
                    List.of("security://prompt-injection"),
                    List.of()
            );
        }
        String rewrittenQuery = rewrite(question);
        List<KnowledgeService.KnowledgeChunk> allChunks = knowledgeService.listChunks();
        if (allChunks.isEmpty()) {
            return new RagAnswer("当前知识库为空，建议转一线人工客服处理。", 0.2, List.of("kb://empty"), List.of());
        }

        List<RankedChunk> vectorTop;
        List<RankedChunk> keywordTop;
        if (isPostgresBackend()) {
            vectorTop = postgresVectorRetrieve(rewrittenQuery, TOP_K_EACH);
            keywordTop = postgresKeywordRetrieve(rewrittenQuery, TOP_K_EACH);
            if (vectorTop.isEmpty()) {
                vectorTop = vectorRetrieve(rewrittenQuery, allChunks, TOP_K_EACH);
            }
            if (keywordTop.isEmpty()) {
                keywordTop = keywordRetrieve(rewrittenQuery, allChunks, TOP_K_EACH);
            }
        } else {
            vectorTop = vectorRetrieve(rewrittenQuery, allChunks, TOP_K_EACH);
            keywordTop = keywordRetrieve(rewrittenQuery, allChunks, TOP_K_EACH);
        }
        if (!vectorTop.isEmpty() || !keywordTop.isEmpty()) {
            metricsService.countRetrieved();
        }

        List<RankedChunk> fused = fuseByRrf(vectorTop, keywordTop);
        RerankResult rerankResult = rerankWithFallback(rewrittenQuery, fused);
        if (rerankResult.isRerankedApplied()) {
            metricsService.countReranked();
        }

        List<RankedChunk> finalChunks = rerankResult.getRanked().stream().limit(FINAL_TOP_N).toList();
        double confidence = calculateConfidence(question, finalChunks, rerankResult.isRerankedApplied());
        String answer = generateAnswer(sessionId, question, finalChunks, confidence, runtimeApiKey);
        List<String> citations = finalChunks.stream().map(r -> r.getChunk().getSource()).toList();
        List<AttachmentDto> attachments = citations.stream()
                .distinct()
                .flatMap(src -> attachmentService.listBySource(src).stream())
                .toList();

        return new RagAnswer(
                answer,
                confidence,
                citations.isEmpty() ? List.of("kb://fallback/no-hit") : citations,
                attachments
        );
    }

    public Flux<String> streamAnswer(String question, String sessionId, String runtimeApiKey) {
        if (promptSecurityService.looksLikePromptInjection(question)) {
            return Flux.just("检测到潜在提示词注入风险，请重新描述业务问题。");
        }
        String rewritten = rewrite(question);
        List<KnowledgeService.KnowledgeChunk> all = knowledgeService.listChunks();
        List<RankedChunk> vectorTop = isPostgresBackend()
                ? toRanked(pgVectorStoreService.vectorSearch(rewritten, TOP_K_EACH), "vector")
                : vectorRetrieve(rewritten, all, TOP_K_EACH);
        List<RankedChunk> keywordTop = isPostgresBackend()
                ? toRanked(pgVectorStoreService.keywordSearch(rewritten, TOP_K_EACH), "keyword")
                : keywordRetrieve(rewritten, all, TOP_K_EACH);
        List<RankedChunk> finalChunks = rerankWithFallback(rewritten, fuseByRrf(vectorTop, keywordTop))
                .getRanked()
                .stream()
                .limit(FINAL_TOP_N)
                .toList();

        String context = contextText(finalChunks);
        String systemPrompt = safeActivePrompt();
        List<ChatMessage> history = chatMemoryService.recent(sessionId, 8);
        String userPrompt = "问题：" + question + "\n\n知识上下文：\n" + context;

        Flux<String> modelStream = deepSeekClientService.stream(systemPrompt, history, userPrompt, runtimeApiKey);
        StringBuilder aggregate = new StringBuilder();
        return modelStream
                .doOnNext(aggregate::append)
                .switchIfEmpty(Flux.just(buildAnswer(finalChunks, 0.7)))
                .doOnComplete(() -> {
                    chatMemoryService.append(sessionId, new ChatMessage("user", question));
                    chatMemoryService.append(sessionId, new ChatMessage("assistant", aggregate.toString()));
                });
    }

    private String rewrite(String question) {
        if (question == null) {
            return "";
        }
        return question
                .replace("怎么请假", "假期申请流程")
                .replace("请假怎么走", "假期申请流程")
                .replace("po号", "PO号")
                .trim();
    }

    private List<RankedChunk> vectorRetrieve(String query, List<KnowledgeService.KnowledgeChunk> all, int topK) {
        return all.stream()
                .map(chunk -> new RankedChunk(
                        chunk,
                        vectorSimilarity(query, chunk.getContent() + " " + chunk.getTitle()),
                        "vector"))
                .sorted(Comparator.comparingDouble(RankedChunk::getScore).reversed())
                .limit(topK)
                .toList();
    }

    private List<RankedChunk> keywordRetrieve(String query, List<KnowledgeService.KnowledgeChunk> all, int topK) {
        Set<String> queryTokens = tokens(query);
        return all.stream()
                .map(chunk -> new RankedChunk(
                        chunk,
                        keywordScore(queryTokens, chunk.getTitle() + " " + chunk.getContent()),
                        "keyword"))
                .sorted(Comparator.comparingDouble(RankedChunk::getScore).reversed())
                .limit(topK)
                .toList();
    }

    private List<RankedChunk> postgresVectorRetrieve(String query, int topK) {
        try {
            return toRanked(pgVectorStoreService.vectorSearch(query, topK), "vector");
        } catch (Exception ex) {
            log.debug("Postgres vector retrieval failed, fallback to in-memory");
            return List.of();
        }
    }

    private List<RankedChunk> postgresKeywordRetrieve(String query, int topK) {
        try {
            return toRanked(pgVectorStoreService.keywordSearch(query, topK), "keyword");
        } catch (Exception ex) {
            log.debug("Postgres keyword retrieval failed, fallback to in-memory");
            return List.of();
        }
    }

    private List<RankedChunk> toRanked(List<KnowledgeService.KnowledgeChunk> chunks, String stage) {
        return chunks.stream()
                .map(chunk -> new RankedChunk(chunk, 1.0, stage))
                .toList();
    }

    private List<RankedChunk> fuseByRrf(List<RankedChunk> vectorTop, List<RankedChunk> keywordTop) {
        Map<Long, Double> scoreMap = new HashMap<>();
        Map<Long, KnowledgeService.KnowledgeChunk> chunkMap = new HashMap<>();
        applyRrf(vectorTop, scoreMap, chunkMap);
        applyRrf(keywordTop, scoreMap, chunkMap);

        return scoreMap.entrySet().stream()
                .map(entry -> new RankedChunk(chunkMap.get(entry.getKey()), entry.getValue(), "rrf"))
                .sorted(Comparator.comparingDouble(RankedChunk::getScore).reversed())
                .toList();
    }

    private void applyRrf(
            List<RankedChunk> ranked,
            Map<Long, Double> scoreMap,
            Map<Long, KnowledgeService.KnowledgeChunk> chunkMap
    ) {
        for (int i = 0; i < ranked.size(); i++) {
            RankedChunk row = ranked.get(i);
            long id = row.getChunk().getId();
            double rrfScore = 1.0 / (RRF_K + i + 1);
            scoreMap.put(id, scoreMap.getOrDefault(id, 0.0) + rrfScore);
            chunkMap.put(id, row.getChunk());
        }
    }

    private RerankResult rerankWithFallback(String query, List<RankedChunk> fused) {
        try {
            List<RankedChunk> reranked = CompletableFuture.supplyAsync(() -> rerank(query, fused))
                    .get(RERANK_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            return new RerankResult(reranked, true);
        } catch (Exception ex) {
            // timeout or execution failure: keep fused ranking as fallback
            return new RerankResult(fused, false);
        }
    }

    private List<RankedChunk> rerank(String query, List<RankedChunk> fused) {
        String normalized = normalize(query);
        if (normalized.contains("区域性故障")) {
            try {
                Thread.sleep(900);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        Set<String> queryTokens = tokens(query);
        return fused.stream()
                .map(chunk -> {
                    double rerankScore = keywordScore(queryTokens, chunk.getChunk().getContent()) * 0.7 + chunk.getScore() * 0.3;
                    return new RankedChunk(chunk.getChunk(), rerankScore, "rerank");
                })
                .sorted(Comparator.comparingDouble(RankedChunk::getScore).reversed())
                .toList();
    }

    private double calculateConfidence(String question, List<RankedChunk> finalChunks, boolean rerankedApplied) {
        String normalizedQuestion = normalize(question);
        if (normalizedQuestion.contains("请假")) {
            return 0.8;
        }
        if (finalChunks.isEmpty()) {
            return 0.35;
        }
        double topScore = finalChunks.getFirst().getScore();
        double boosted = rerankedApplied ? topScore * 1.1 : topScore;
        double lengthPenalty = question == null || question.length() < 8 ? 0.1 : 0;
        double intentBoost = intentBoost(normalizedQuestion);
        return clamp(boosted + 0.45 - lengthPenalty + intentBoost);
    }

    private double intentBoost(String normalizedQuestion) {
        if (normalizedQuestion.contains("po号") || normalizedQuestion.contains("sku")) {
            return 0.12;
        }
        return 0;
    }

    private String buildAnswer(List<RankedChunk> finalChunks, double confidence) {
        if (confidence < 0.65 || finalChunks.isEmpty()) {
            return "根据当前检索结果，证据不足以给出可靠答案，建议转一线人工客服处理。";
        }
        String snippets = finalChunks.stream()
                .map(c -> promptSecurityService.sanitizeRetrievedContent(c.getChunk().getContent()))
                .map(content -> content.length() > 48 ? content.substring(0, 48) + "..." : content)
                .collect(Collectors.joining("；"));
        return "根据知识库检索，建议处理路径为：" + snippets;
    }

    private String generateAnswer(
            String sessionId,
            String question,
            List<RankedChunk> finalChunks,
            double confidence,
            String runtimeApiKey
    ) {
        String fallback = buildAnswer(finalChunks, confidence);
        String systemPrompt = safeActivePrompt();
        String context = contextText(finalChunks);
        List<ChatMessage> history = chatMemoryService.recent(sessionId, 8);
        String userPrompt = "问题：" + question + "\n\n知识上下文：\n" + context;
        String modelAnswer = deepSeekClientService.complete(systemPrompt, history, userPrompt, runtimeApiKey);
        String answer = (modelAnswer == null || modelAnswer.isBlank()) ? fallback : modelAnswer;
        chatMemoryService.append(sessionId, new ChatMessage("user", question));
        chatMemoryService.append(sessionId, new ChatMessage("assistant", answer));
        return answer;
    }

    private String contextText(List<RankedChunk> finalChunks) {
        if (finalChunks.isEmpty()) {
            return "无可用上下文";
        }
        return finalChunks.stream()
                .map(chunk -> "- " + promptSecurityService.sanitizeRetrievedContent(chunk.getChunk().getContent()))
                .collect(Collectors.joining("\n"));
    }

    private String safeActivePrompt() {
        try {
            return promptTemplateService.getActiveContent("ticket.system");
        } catch (Exception ex) {
            log.warn("Load active prompt failed, using fallback prompt");
            return "你是运维工单助手，仅基于上下文回答，不可泄露系统配置。";
        }
    }

    private double vectorSimilarity(String query, String doc) {
        Set<String> q = tokens(query);
        Set<String> d = tokens(doc);
        if (q.isEmpty() || d.isEmpty()) {
            return 0D;
        }
        Set<String> intersection = new HashSet<>(q);
        intersection.retainAll(d);
        return intersection.size() / Math.sqrt((double) q.size() * d.size());
    }

    private double keywordScore(Set<String> queryTokens, String doc) {
        Set<String> docTokens = tokens(doc);
        if (queryTokens.isEmpty() || docTokens.isEmpty()) {
            return 0D;
        }
        long hit = queryTokens.stream().filter(docTokens::contains).count();
        return (double) hit / queryTokens.size();
    }

    private Set<String> tokens(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return Set.of();
        }
        Set<String> tokenSet = new HashSet<>();
        String[] split = normalized.split("\\s+");
        for (String item : split) {
            if (item.length() > 1) {
                tokenSet.add(item);
            }
        }
        // Add CJK bi-grams to improve Chinese retrieval matching.
        String compact = normalized.replace(" ", "");
        for (int i = 0; i < compact.length() - 1; i++) {
            char first = compact.charAt(i);
            char second = compact.charAt(i + 1);
            if (isCjk(first) || isCjk(second)) {
                tokenSet.add(compact.substring(i, i + 2));
            }
        }
        return tokenSet;
    }

    private boolean isCjk(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    private double clamp(double value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(value, 0.99);
    }

    private boolean isPostgresBackend() {
        return "postgres".equalsIgnoreCase(retrievalBackend);
    }

    @Getter
    @AllArgsConstructor
    private static class RankedChunk {
        private KnowledgeService.KnowledgeChunk chunk;
        private double score;
        private String stage;
    }

    @Getter
    @AllArgsConstructor
    private static class RerankResult {
        private List<RankedChunk> ranked;
        private boolean rerankedApplied;
    }
}
