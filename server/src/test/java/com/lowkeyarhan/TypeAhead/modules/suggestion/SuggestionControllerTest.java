package com.lowkeyarhan.TypeAhead.modules.suggestion;

import com.lowkeyarhan.TypeAhead.modules.suggestion.controller.SuggestionController;
import com.lowkeyarhan.TypeAhead.modules.suggestion.service.SuggestionService;

import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestResultDTO;
import com.lowkeyarhan.TypeAhead.modules.suggestion.dto.SuggestionResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Unit test verifying controller parameter processing, normalization, and response payload formatting.
class SuggestionControllerTest {

    private SuggestionService suggestionService;
    private SuggestionController controller;

    @BeforeEach
    void setUp() {
        suggestionService = Mockito.mock(SuggestionService.class);
        controller = new SuggestionController(suggestionService);
    }

    @Test
    void testSuggestEndpointSuccess() {
        List<SuggestResultDTO> suggestions = List.of(
                new SuggestResultDTO("iphone", 100L));
        when(suggestionService.getSuggestions("iphone", 10)).thenReturn(suggestions);

        ResponseEntity<SuggestionResponseDTO> response = controller.suggest("IpHoNe", 10);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        SuggestionResponseDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.prefix()).isEqualTo("iphone");
        assertThat(body.suggestions()).hasSize(1);
        assertThat(body.suggestions().get(0).query()).isEqualTo("iphone");
        assertThat(body.suggestions().get(0).count()).isEqualTo(100L);
    }

    @Test
    void testSuggestEndpointEmptyQuery() {
        ResponseEntity<SuggestionResponseDTO> response = controller.suggest("   ", 10);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        SuggestionResponseDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.prefix()).isEmpty();
        assertThat(body.suggestions()).isEmpty();
    }

    @Test
    void testSuggestEndpointNullQuery() {
        ResponseEntity<SuggestionResponseDTO> response = controller.suggest(null, 10);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        SuggestionResponseDTO body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.prefix()).isEmpty();
        assertThat(body.suggestions()).isEmpty();
    }
}
