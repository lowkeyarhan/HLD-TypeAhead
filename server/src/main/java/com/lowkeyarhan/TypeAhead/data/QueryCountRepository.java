package com.lowkeyarhan.TypeAhead.data;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository interface for managing QueryCount entities.
 */
@Repository
public interface QueryCountRepository extends JpaRepository<QueryCount, Long> {

        /**
         * Finds query statistics by exact query text match.
         *
         * @param queryText the normalized query string
         * @return the query count entity if present
         */
        Optional<QueryCount> findByQueryText(String queryText);

        /**
         * Performs a single-row native upsert of a query count.
         * If the query text already exists, increments the counts and updates the last
         * searched timestamp.
         *
         * Note: Phase 9 will implement a batch version of this statement to support
         * bulk flushes.
         *
         * @param queryText      the normalized query text
         * @param totalCount     the count increment to apply for total searches
         * @param recentCount    the count increment to apply for recent searches
         * @param lastSearchedAt the timestamp of the search
         * @param createdAt      the timestamp of creation (ignored on conflict updates)
         */
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
