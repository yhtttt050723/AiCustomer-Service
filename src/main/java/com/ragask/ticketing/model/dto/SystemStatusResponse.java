package com.ragask.ticketing.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * System status response payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusResponse {
    private boolean deepSeekEnabled;
    private String deepSeekModel;
    private boolean embeddingOnlineEnabled;
    private boolean redisMemoryEnabled;
    private boolean redisReachable;
    private String retrievalBackend;
    private boolean pgvectorReady;
    private Integer activePromptVersion;
}
