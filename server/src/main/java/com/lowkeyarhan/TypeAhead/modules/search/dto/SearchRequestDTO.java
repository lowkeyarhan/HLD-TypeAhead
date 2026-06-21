package com.lowkeyarhan.TypeAhead.modules.search.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Request DTO containing search query string.
public record SearchRequestDTO(
        @NotBlank(message = "Query must not be blank") @Size(max = 100, message = "Query length must not exceed 100 characters") String query) {
}
