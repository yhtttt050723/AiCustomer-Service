package com.ragask.ticketing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragask.ticketing.model.dto.ChatMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
public class DeepSeekClientService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String reasoningEffort;

    public DeepSeekClientService(
            ObjectMapper objectMapper,
            @Value("${deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${deepseek.api-key:}") String apiKey,
            @Value("${deepseek.model:deepseek-v4-pro}") String model,
            @Value("${deepseek.reasoning-effort:high}") String reasoningEffort
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.reasoningEffort = reasoningEffort;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String modelName() {
        return model;
    }

    public String complete(String systemPrompt, List<ChatMessage> history, String userPrompt) {
        return complete(systemPrompt, history, userPrompt, null);
    }

    public String complete(
            String systemPrompt,
            List<ChatMessage> history,
            String userPrompt,
            String overrideApiKey
    ) {
        String effectiveKey = resolveApiKey(overrideApiKey);
        if (effectiveKey == null || effectiveKey.isBlank()) {
            return null;
        }
        Map<String, Object> body = basePayload(systemPrompt, history, userPrompt, false);
        JsonNode root = webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + effectiveKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();
        if (root == null || root.path("choices").isEmpty()) {
            return null;
        }
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    public Flux<String> stream(String systemPrompt, List<ChatMessage> history, String userPrompt) {
        return stream(systemPrompt, history, userPrompt, null);
    }

    public Flux<String> stream(
            String systemPrompt,
            List<ChatMessage> history,
            String userPrompt,
            String overrideApiKey
    ) {
        String effectiveKey = resolveApiKey(overrideApiKey);
        if (effectiveKey == null || effectiveKey.isBlank()) {
            return Flux.empty();
        }
        Map<String, Object> body = basePayload(systemPrompt, history, userPrompt, true);
        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + effectiveKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::parseStreamChunk)
                .filter(text -> text != null && !text.isBlank());
    }

    private String resolveApiKey(String overrideApiKey) {
        if (overrideApiKey != null && !overrideApiKey.isBlank()) {
            return overrideApiKey;
        }
        return apiKey;
    }

    private Map<String, Object> basePayload(
            String systemPrompt,
            List<ChatMessage> history,
            String userPrompt,
            boolean stream
    ) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessage item : history) {
            messages.add(Map.of("role", item.role(), "content", item.content()));
        }
        messages.add(Map.of("role", "user", "content", userPrompt));

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("thinking", Map.of("type", "enabled"));
        payload.put("reasoning_effort", reasoningEffort);
        payload.put("stream", stream);
        return payload;
    }

    private String parseStreamChunk(String raw) {
        String[] lines = raw.split("\\R");
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("data:")) {
                continue;
            }
            String body = trimmed.substring(5).trim();
            if ("[DONE]".equals(body)) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(body);
                JsonNode delta = node.path("choices").get(0).path("delta").path("content");
                if (!delta.isMissingNode()) {
                    out.append(delta.asText());
                }
            } catch (Exception ignored) {
                // best-effort streaming parsing
            }
        }
        return out.toString();
    }
}
