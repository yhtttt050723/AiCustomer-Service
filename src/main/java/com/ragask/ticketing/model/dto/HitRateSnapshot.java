package com.ragask.ticketing.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Retrieval and resolution metric snapshot.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HitRateSnapshot {
    private long totalQueries;
    private long retrievedQueries;
    private long rerankedQueries;
    private long resolvedByAi;
    private double retrievalHitRate;
    private double rerankCoverageRate;
    private double aiResolutionRate;
}
