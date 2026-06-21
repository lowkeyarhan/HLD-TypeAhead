package com.lowkeyarhan.TypeAhead.modules.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

// Entity representing search query statistics.
// 
// Note: queryText is expected to be normalized (trimmed and lowercase) BEFORE
// it reaches this layer. This normalization is handled in a shared utility.
@Entity
@Table(name = "query_count")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query_text", nullable = false, unique = true)
    private String queryText;

    @Column(name = "total_count", nullable = false)
    @Builder.Default
    private Long totalCount = 0L;

    @Column(name = "recent_count", nullable = false)
    @Builder.Default
    private Long recentCount = 0L;

    @Column(name = "last_searched_at")
    private Instant lastSearchedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
