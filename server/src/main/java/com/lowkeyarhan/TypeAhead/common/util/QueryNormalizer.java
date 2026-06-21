package com.lowkeyarhan.TypeAhead.common.util;

// Shared utility class to normalize query search texts across suggestions and search endpoints.
public final class QueryNormalizer {

    private static final int MAX_QUERY_LENGTH = 100;

    private QueryNormalizer() {}

    // Normalizes raw user input by trimming, converting to lowercase, removing control characters,
    // and capping query length to a maximum threshold.
    public static String normalize(String query) {
        if (query == null) {
            return "";
        }
        // Remove all Control characters defensively
        String cleaned = query.replaceAll("\\p{Cntrl}", "");
        String trimmed = cleaned.trim().toLowerCase();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            return trimmed.substring(0, MAX_QUERY_LENGTH);
        }
        return trimmed;
    }
}
