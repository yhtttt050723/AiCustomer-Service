package com.ragask.ticketing.model.dto;

public record HitRateSnapshot(
        long totalQueries,
        long retrievedQueries,
        long rerankedQueries,
        long resolvedByAi,
        double retrievalHitRate,
        double rerankCoverageRate,
        double aiResolutionRate
) {
}
