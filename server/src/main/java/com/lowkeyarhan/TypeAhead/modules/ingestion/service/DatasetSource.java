package com.lowkeyarhan.TypeAhead.modules.ingestion.service;

import com.lowkeyarhan.TypeAhead.modules.ingestion.dto.QueryCountRow;
import java.util.stream.Stream;

// Strategy interface representing a source of query count records.
public interface DatasetSource {
    Stream<QueryCountRow> load();
}
