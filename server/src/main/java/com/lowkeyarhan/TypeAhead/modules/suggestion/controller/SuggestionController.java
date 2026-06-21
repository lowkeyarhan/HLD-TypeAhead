package com.lowkeyarhan.TypeAhead.modules.suggestion.controller;

import com.lowkeyarhan.TypeAhead.common.util.QueryNormalizer;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestionResponseDTO;
import com.lowkeyarhan.TypeAhead.modules.suggestion.service.SuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

// Controller exposing endpoint to retrieve suggestions for a given search query prefix.
@Tag(name = "Suggestion API", description = "Endpoints for query autocompletion suggestions")
@RestController
public class SuggestionController {

    private final SuggestionService suggestionService;

    public SuggestionController(SuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @Operation(summary = "Get query suggestions matching prefix")
    @GetMapping("/suggest")
    public ResponseEntity<SuggestionResponseDTO> suggest(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "limit", required = false, defaultValue = "10") int limit) {

        String normalized = QueryNormalizer.normalize(q);
        if (normalized.isEmpty()) {
            return ResponseEntity.ok(new SuggestionResponseDTO("", List.of()));
        }

        List<SuggestResultDTO> suggestions = suggestionService.getSuggestions(normalized, limit);
        return ResponseEntity.ok(new SuggestionResponseDTO(normalized, suggestions));
    }
}
