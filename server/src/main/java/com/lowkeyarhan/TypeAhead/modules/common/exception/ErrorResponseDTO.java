package com.lowkeyarhan.TypeAhead.modules.common.exception;

import java.time.Instant;

// DTO representing standard error responses across all API endpoints.
public record ErrorResponseDTO(
    Instant timestamp,
    String message,
    String path
) {}
