package com.lowkeyarhan.TypeAhead.modules.ingestion;

import java.util.stream.Stream;

// Strategy interface representing a source of query count records.
public interface DatasetSource {
    Stream<QueryCountRow> load();
}
