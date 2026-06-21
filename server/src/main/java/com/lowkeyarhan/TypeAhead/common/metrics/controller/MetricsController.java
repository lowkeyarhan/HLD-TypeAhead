package com.lowkeyarhan.TypeAhead.common.metrics.controller;

import com.lowkeyarhan.TypeAhead.common.metrics.MetricsService;
import com.lowkeyarhan.TypeAhead.common.metrics.MetricsResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// REST Controller exposing system metrics and telemetry.
@Tag(name = "Metrics API", description = "Endpoints for system metrics and diagnostics")
@RestController
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @Operation(summary = "Get current system performance and operation metrics")
    @GetMapping("/metrics")
    public ResponseEntity<MetricsResponseDTO> getMetrics() {
        MetricsResponseDTO dto = new MetricsResponseDTO(
                metricsService.getSuggestP95LatencyMs(),
                metricsService.getOverallCacheHitRate(),
                metricsService.getPerNodeCacheHitRates(),
                metricsService.getDbReadCount(),
                metricsService.getDbWriteCount(),
                metricsService.getRequestsToFlushesRatio()
        );
        return ResponseEntity.ok(dto);
    }
}
