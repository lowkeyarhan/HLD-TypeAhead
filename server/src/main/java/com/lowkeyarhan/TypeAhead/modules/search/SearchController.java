package com.lowkeyarhan.TypeAhead.modules.search;

import com.lowkeyarhan.TypeAhead.modules.search.dto.SearchRequestDTO;
import com.lowkeyarhan.TypeAhead.modules.search.dto.SearchResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Controller exposing search query submission endpoints.
@Tag(name = "Search API", description = "Endpoints for query tracking and search submissions")
@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @Operation(summary = "Submit a search query")
    @PostMapping("/search")
    public ResponseEntity<SearchResponseDTO> search(@Valid @RequestBody SearchRequestDTO request) {
        searchService.submitSearch(request.query());
        return ResponseEntity.ok(new SearchResponseDTO("Searched"));
    }
}
