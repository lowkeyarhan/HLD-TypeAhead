package com.lowkeyarhan.TypeAhead.modules.ingestion;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

// Unit test verifying CSV parsing and bad data resilience.
class CsvDatasetSourceTest {

    @Test
    void testCsvDatasetSourceSkipsMalformedRows() {
        CsvDatasetSource source = new CsvDatasetSource("dataset/test_queries.csv");
        List<QueryCountRow> rows = source.load().collect(Collectors.toList());

        assertThat(rows).hasSize(3);

        assertThat(rows.get(0).queryText()).isEqualTo("iphone");
        assertThat(rows.get(0).count()).isEqualTo(100L);

        assertThat(rows.get(1).queryText()).isEqualTo("java tutorial");
        assertThat(rows.get(1).count()).isEqualTo(50L);

        assertThat(rows.get(2).queryText()).isEqualTo("valid space");
        assertThat(rows.get(2).count()).isEqualTo(20L);
    }
}
