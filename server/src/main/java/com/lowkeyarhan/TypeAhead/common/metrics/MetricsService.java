package com.lowkeyarhan.TypeAhead.common.metrics;

import java.util.Map;

// Service interface exposing telemetry indicators including latency percentiles, cache rates, and DB read/write stats.
public interface MetricsService {

    void recordSuggestLatency(long durationNanos);

    double getSuggestP95LatencyMs();

    double getOverallCacheHitRate();

    Map<String, Double> getPerNodeCacheHitRates();

    long getDbReadCount();

    long getDbWriteCount();

    void incrementDbReads(long delta);

    void incrementDbWrites(long delta);

    double getRequestsToFlushesRatio();
}
