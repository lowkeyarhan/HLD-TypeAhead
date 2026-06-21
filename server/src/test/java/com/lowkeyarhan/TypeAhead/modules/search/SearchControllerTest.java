package com.lowkeyarhan.TypeAhead.modules.search;

import com.lowkeyarhan.TypeAhead.modules.search.controller.SearchController;
import com.lowkeyarhan.TypeAhead.modules.search.service.SearchService;

import com.lowkeyarhan.TypeAhead.modules.search.dto.SearchRequestDTO;
import com.lowkeyarhan.TypeAhead.modules.search.dto.SearchResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// Unit test verifying SearchController parameter processing.
class SearchControllerTest {

    private SearchService searchService;
    private SearchController controller;

    @BeforeEach
    void setUp() {
        searchService = Mockito.mock(SearchService.class);
        controller = new SearchController(searchService);
    }

    @Test
    void testSearchControllerDelegatesToServiceAndReturnsMessage() {
        SearchRequestDTO request = new SearchRequestDTO("query string");
        ResponseEntity<SearchResponseDTO> response = controller.search(request);

        verify(searchService, times(1)).submitSearch("query string");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Searched");
    }
}
