package com.lowkeyarhan.TypeAhead.modules.ingestion.dto;

// Data record representing a query and its search frequency count.
public record QueryCountRow(String queryText, Long count) {}
