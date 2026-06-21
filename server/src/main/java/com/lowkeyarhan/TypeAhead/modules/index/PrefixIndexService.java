package com.lowkeyarhan.TypeAhead.modules.index;

import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;

import java.time.Instant;
import java.util.List;

// Service interface exposing index search, incremental updates, and rebuild triggers.
public interface PrefixIndexService {

    // Performs a prefix search, returning matches sorted by score.
    //
    // @param prefix the prefix string to query
    // @param limit the maximum results to return
    // @return list of matching QueryCount objects
    List<QueryCount> search(String prefix, int limit);

    // Applies an incremental change (delta increment) to a query's count.
    //
    // @param queryText the query text to update
    // @param countDelta the count delta to add
    // @param searchedAt the timestamp of the search
    void applyDelta(String queryText, long countDelta, Instant searchedAt);

    // Rebuilds the in-memory prefix index from the database.
    void rebuild();

    // Retrieves all candidates from the index.
    List<QueryCount> getAllCandidates();
}
