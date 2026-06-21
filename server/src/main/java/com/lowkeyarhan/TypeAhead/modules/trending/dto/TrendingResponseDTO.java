package com.lowkeyarhan.TypeAhead.modules.trending.dto;

import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import java.util.List;

// DTO representing the response from the trending queries endpoint.
public record TrendingResponseDTO(List<SuggestResultDTO> trending) {}
