package com.ragask.ticketing.model.dto;

public record SystemStatusResponse(
        boolean deepSeekEnabled,
        String deepSeekModel,
        boolean embeddingOnlineEnabled,
        boolean redisMemoryEnabled,
        boolean redisReachable,
        String retrievalBackend,
        boolean pgvectorReady,
        Integer activePromptVersion
) {
}
