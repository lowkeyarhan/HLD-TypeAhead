package com.lowkeyarhan.TypeAhead.modules.ingestion;

import com.lowkeyarhan.TypeAhead.modules.ingestion.dto.QueryCountRow;
import com.lowkeyarhan.TypeAhead.modules.ingestion.service.impl.SyntheticDatasetSource;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;

// Unit test verifying synthetic data generation rules.
class SyntheticDatasetSourceTest {

    @Test
    void testSyntheticSourceGeneratesRequestedCountAndUniqueQueries() {
        int requestedCount = 1000;
        SyntheticDatasetSource source = new SyntheticDatasetSource(requestedCount);
        List<QueryCountRow> rows = source.load().collect(Collectors.toList());

        assertThat(rows).hasSize(requestedCount);

        Set<String> uniqueQueries = rows.stream()
                .map(QueryCountRow::queryText)
                .collect(Collectors.toSet());
        assertThat(uniqueQueries).hasSize(requestedCount);

        for (QueryCountRow row : rows) {
            assertThat(row.queryText()).isNotBlank();
            assertThat(row.count()).isPositive();
        }
    }
}
