package com.lowkeyarhan.TypeAhead.modules.cache.dto;

// DTO holding cache routing diagnostics: owning node ID and whether the lookup was a cache hit.
public record CacheDebugInfo(String nodeId, boolean hit) {
}
