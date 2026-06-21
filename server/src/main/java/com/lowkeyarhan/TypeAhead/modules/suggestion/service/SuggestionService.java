package com.lowkeyarhan.TypeAhead.modules.suggestion.service;

import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import java.util.List;

// Service interface defining the search suggestion retrieval.
public interface SuggestionService {
    List<SuggestResultDTO> getSuggestions(String prefix, int limit);
}
