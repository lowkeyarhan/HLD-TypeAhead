package com.lowkeyarhan.TypeAhead.modules.cache;

import com.lowkeyarhan.TypeAhead.common.util.QueryNormalizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// REST Controller exposing endpoints to inspect consistent hashing ring and cache allocation.
@Tag(name = "Cache API", description = "Endpoints for cache diagnostics")
@RestController
public class CacheController {

    private final CacheNodeManager cacheNodeManager;

    public CacheController(CacheNodeManager cacheNodeManager) {
        this.cacheNodeManager = cacheNodeManager;
    }

    @Operation(summary = "Get cache allocation diagnostics for a query prefix")
    @GetMapping("/cache/debug")
    public ResponseEntity<CacheDebugResponseDTO> getDebugInfo(
            @RequestParam("prefix") String prefix,
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit) {

        String normalized = QueryNormalizer.normalize(prefix);
        // Build the exact same cache key as SuggestionService
        String cacheKey = "suggest:" + normalized + ":" + limit;
        CacheDebugInfo debugInfo = cacheNodeManager.getDebugInfo(cacheKey);
        
        return ResponseEntity.ok(new CacheDebugResponseDTO(debugInfo.nodeId(), debugInfo.hit()));
    }
}
