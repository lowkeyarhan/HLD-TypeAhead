package com.lowkeyarhan.TypeAhead.modules.data;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Integration tests for QueryCountRepository.
// Integration tests requiring PostgreSQL running on localhost:5432.
@SpringBootTest
@Transactional
class QueryCountRepositoryTest {

    @Autowired
    private QueryCountRepository repository;

    @Test
    void testSaveAndFindByQueryText() {
        QueryCount qc = QueryCount.builder()
                .queryText("test-query")
                .totalCount(10L)
                .recentCount(2L)
                .createdAt(Instant.now())
                .build();

        repository.save(qc);

        Optional<QueryCount> found = repository.findByQueryText("test-query");
        assertThat(found).isPresent();
        assertThat(found.get().getQueryText()).isEqualTo("test-query");
        assertThat(found.get().getTotalCount()).isEqualTo(10L);
    }

    @Test
    void testUniqueConstraintViolation() {
        QueryCount qc1 = QueryCount.builder()
                .queryText("duplicate-query")
                .totalCount(5L)
                .createdAt(Instant.now())
                .build();
        repository.saveAndFlush(qc1);

        QueryCount qc2 = QueryCount.builder()
                .queryText("duplicate-query")
                .totalCount(3L)
                .createdAt(Instant.now())
                .build();

        assertThatThrownBy(() -> repository.saveAndFlush(qc2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testUpsertSingle() {
        Instant now = Instant.now();

        // First upsert inserts a new row
        repository.upsertSingle("upsert-query", 1L, 1L, now, now);

        Optional<QueryCount> found = repository.findByQueryText("upsert-query");
        assertThat(found).isPresent();
        assertThat(found.get().getTotalCount()).isEqualTo(1);
        assertThat(found.get().getRecentCount()).isEqualTo(1);

        // Second upsert increments the existing row
        repository.upsertSingle("upsert-query", 2L, 1L, now, now);

        Optional<QueryCount> foundAgain = repository.findByQueryText("upsert-query");
        assertThat(foundAgain).isPresent();
        assertThat(foundAgain.get().getTotalCount()).isEqualTo(3);
        assertThat(foundAgain.get().getRecentCount()).isEqualTo(2);
    }
}
