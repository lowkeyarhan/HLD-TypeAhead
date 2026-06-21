package com.lowkeyarhan.TypeAhead.modules.suggestion.dto;

// DTO representing a single suggestion query result and its score count.
public record SuggestResultDTO(String query, long count) {
}
