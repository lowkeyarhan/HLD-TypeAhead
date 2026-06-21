package com.lowkeyarhan.TypeAhead.modules.trending.controller;

import com.lowkeyarhan.TypeAhead.modules.trending.dto.TrendingResponseDTO;
import com.lowkeyarhan.TypeAhead.modules.trending.service.TrendingService;
import com.lowkeyarhan.TypeAhead.modules.trending.mapper.TrendingMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// REST Controller exposing endpoints to fetch trending queries.
@Tag(name = "Trending API", description = "Endpoints for trending query retrieval")
@RestController
public class TrendingController {

    private final TrendingService trendingService;

    public TrendingController(TrendingService trendingService) {
        this.trendingService = trendingService;
    }

    @Operation(summary = "Get top trending queries based on the active ranking strategy")
    @GetMapping("/trending")
    public ResponseEntity<TrendingResponseDTO> getTrending(
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit) {

        return ResponseEntity.ok(TrendingMapper.toResponseDTO(trendingService.getTrending(limit)));
    }
}
