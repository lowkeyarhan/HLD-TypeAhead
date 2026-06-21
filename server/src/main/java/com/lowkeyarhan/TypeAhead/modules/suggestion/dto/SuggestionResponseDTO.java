package com.lowkeyarhan.TypeAhead.modules.suggestion.dto;

import java.util.List;

// Wrapper DTO representing the suggestions returned for a given prefix.
public record SuggestionResponseDTO(String prefix, List<SuggestResultDTO> suggestions) {}
