package com.lowkeyarhan.TypeAhead.modules.index.service;

import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import java.time.Instant;
import java.util.List;

// Service interface exposing index search, incremental updates, and rebuild triggers.
public interface PrefixIndexService {

    // Performs a prefix search returning matches sorted by score.
    List<QueryCount> search(String prefix, int limit);

    // Applies an incremental delta to a query's count in the in-memory index.
    void applyDelta(String queryText, long countDelta, Instant searchedAt);

    // Rebuilds the in-memory prefix index from the database.
    void rebuild();

    // Retrieves all candidates from the index.
    List<QueryCount> getAllCandidates();
}
