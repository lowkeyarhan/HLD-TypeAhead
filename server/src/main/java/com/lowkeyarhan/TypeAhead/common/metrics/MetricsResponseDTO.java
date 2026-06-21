package com.lowkeyarhan.TypeAhead.common.metrics;

import java.util.Map;

// DTO representing the system performance metrics.
public record MetricsResponseDTO(
        double suggestP95LatencyMs,
        double overallCacheHitRate,
        Map<String, Double> perNodeCacheHitRates,
        long dbReadCount,
        long dbWriteCount,
        double requestsToFlushesRatio
) {}
