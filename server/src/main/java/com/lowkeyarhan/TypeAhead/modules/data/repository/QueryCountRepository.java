package com.lowkeyarhan.TypeAhead.modules.data.repository;

import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

// Repository interface for managing QueryCount entities.
@Repository
public interface QueryCountRepository extends JpaRepository<QueryCount, Long> {

        // Finds query statistics by exact normalized query text.
        Optional<QueryCount> findByQueryText(String queryText);

        // Native upsert: inserts or increments counts for an existing query row.
        @Modifying
        @Query(value = "INSERT INTO query_count (query_text, total_count, recent_count, last_searched_at, created_at) "
                        +
                        "VALUES (:queryText, :totalCount, :recentCount, :lastSearchedAt, :createdAt) " +
                        "ON CONFLICT (query_text) DO UPDATE SET " +
                        "total_count = query_count.total_count + EXCLUDED.total_count, " +
                        "recent_count = query_count.recent_count + EXCLUDED.recent_count, " +
                        "last_searched_at = EXCLUDED.last_searched_at", nativeQuery = true)
        void upsertSingle(@Param("queryText") String queryText,
                        @Param("totalCount") Long totalCount,
                        @Param("recentCount") Long recentCount,
                        @Param("lastSearchedAt") Instant lastSearchedAt,
                        @Param("createdAt") Instant createdAt);
}
