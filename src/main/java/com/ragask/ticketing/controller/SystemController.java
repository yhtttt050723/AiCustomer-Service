package com.ragask.ticketing.controller;

import com.ragask.ticketing.knowledge.EmbeddingService;
import com.ragask.ticketing.knowledge.PgVectorStoreService;
import com.ragask.ticketing.model.dto.SystemPanelResponse;
import com.ragask.ticketing.model.dto.SystemStatusResponse;
import com.ragask.ticketing.model.dto.DeepSeekTestRequest;
import com.ragask.ticketing.repository.ConversationRecordRepository;
import com.ragask.ticketing.service.ChatMemoryService;
import com.ragask.ticketing.service.DeepSeekClientService;
import com.ragask.ticketing.service.EscalationDispatchService;
import com.ragask.ticketing.service.PromptTemplateService;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final DeepSeekClientService deepSeekClientService;
    private final EmbeddingService embeddingService;
    private final ChatMemoryService chatMemoryService;
    private final PgVectorStoreService pgVectorStoreService;
    private final PromptTemplateService promptTemplateService;
    private final EscalationDispatchService escalationDispatchService;
    private final ConversationRecordRepository conversationRecordRepository;
    private final String retrievalBackend;

    public SystemController(
            DeepSeekClientService deepSeekClientService,
            EmbeddingService embeddingService,
            ChatMemoryService chatMemoryService,
            PgVectorStoreService pgVectorStoreService,
            PromptTemplateService promptTemplateService,
            EscalationDispatchService escalationDispatchService,
            ConversationRecordRepository conversationRecordRepository,
            @Value("${rag.retrieval.backend:memory}") String retrievalBackend
    ) {
        this.deepSeekClientService = deepSeekClientService;
        this.embeddingService = embeddingService;
        this.chatMemoryService = chatMemoryService;
        this.pgVectorStoreService = pgVectorStoreService;
        this.promptTemplateService = promptTemplateService;
        this.escalationDispatchService = escalationDispatchService;
        this.conversationRecordRepository = conversationRecordRepository;
        this.retrievalBackend = retrievalBackend;
    }

    @GetMapping("/status")
    public SystemStatusResponse status() {
        return new SystemStatusResponse(
                deepSeekClientService.enabled(),
                deepSeekClientService.modelName(),
                embeddingService.onlineAvailable(),
                chatMemoryService.redisMemoryEnabled(),
                chatMemoryService.redisReachable(),
                retrievalBackend,
                pgVectorStoreService.pgvectorReady(),
                promptTemplateService.getActiveVersionNo("ticket.system")
        );
    }

    @GetMapping("/panel")
    public SystemPanelResponse panel() {
        String l1Url = escalationDispatchService.getL1Url();
        String l2Url = escalationDispatchService.getL2Url();
        return new SystemPanelResponse(
                l1Url,
                l2Url,
                parsePort(l1Url),
                parsePort(l2Url),
                escalationDispatchService.recentEvents(),
                conversationRecordRepository.findTop100ByOrderByCreatedAtDesc()
        );
    }

    @PostMapping("/deepseek/test")
    public String testDeepSeek(@RequestBody DeepSeekTestRequest request) {
        try {
            String result = deepSeekClientService.complete(
                    "You are a helpful assistant.",
                    List.of(),
                    "Reply with exactly: OK",
                    request.apiKey()
            );
            if (result == null || result.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DeepSeek test failed (empty response)");
            }
            return result;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DeepSeek test failed: " + ex.getMessage());
        }
    }

    private Integer parsePort(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url);
            int port = uri.getPort();
            if (port > 0) {
                return port;
            }
            return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
        } catch (Exception ignored) {
            return null;
        }
    }
}
