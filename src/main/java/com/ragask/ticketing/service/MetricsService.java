package com.ragask.ticketing.service;

import com.ragask.ticketing.model.dto.HitRateSnapshot;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final AtomicLong totalQueries = new AtomicLong();
    private final AtomicLong retrievedQueries = new AtomicLong();
    private final AtomicLong rerankedQueries = new AtomicLong();
    private final AtomicLong resolvedByAi = new AtomicLong();

    public void countQuery() {
        totalQueries.incrementAndGet();
    }

    public void countRetrieved() {
        retrievedQueries.incrementAndGet();
    }

    public void countReranked() {
        rerankedQueries.incrementAndGet();
    }

    public void countResolvedByAi() {
        resolvedByAi.incrementAndGet();
    }

    public HitRateSnapshot snapshot() {
        long total = totalQueries.get();
        long retrieved = retrievedQueries.get();
        long reranked = rerankedQueries.get();
        long aiSolved = resolvedByAi.get();
        return new HitRateSnapshot(
                total,
                retrieved,
                reranked,
                aiSolved,
                rate(retrieved, total),
                rate(reranked, total),
                rate(aiSolved, total)
        );
    }

    private double rate(long numerator, long denominator) {
        if (denominator == 0) {
            return 0D;
        }
        return (double) numerator / denominator;
    }
}
