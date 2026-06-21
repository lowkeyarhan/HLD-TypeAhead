package com.lowkeyarhan.TypeAhead.modules.ingestion;

// Data record representing a query and its search frequency count.
public record QueryCountRow(String queryText, Long count) {}
