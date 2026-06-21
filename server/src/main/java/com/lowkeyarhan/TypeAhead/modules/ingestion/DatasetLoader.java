package com.lowkeyarhan.TypeAhead.modules.ingestion;

import com.lowkeyarhan.TypeAhead.modules.data.QueryCountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Application runner that loads data from dataset sources on startup.
// Safe across restarts (idempotent checks). Disabled in test profile to maintain speed.
@Slf4j
@Component
@Profile("!test")
public class DatasetLoader implements ApplicationRunner {

    private final QueryCountRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final ApplicationEventPublisher publisher;

    public DatasetLoader(QueryCountRepository repository, JdbcTemplate jdbcTemplate, Clock clock,
            ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.publisher = publisher;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long countInDb = repository.count();
        if (countInDb > 0) {
            log.info("Database already contains {} queries. Skipping ingestion.", countInDb);
            // Trigger prefix index rebuild even if database already had data on start
            publisher.publishEvent(new DatasetLoadedEvent(this));
            return;
        }

        log.info("Starting dataset ingestion...");
        long startTime = System.currentTimeMillis();

        // Choose dataset source
        DatasetSource source = new CsvDatasetSource("dataset/queries.csv");
        Stream<QueryCountRow> stream = source.load();

        // Check if stream is empty (meaning CSV is missing or empty)
        List<QueryCountRow> rows = stream.collect(Collectors.toList());
        if (rows.isEmpty()) {
            log.info("Falling back to SyntheticDatasetSource for generating 100,000+ queries.");
            source = new SyntheticDatasetSource(100000);
            rows = source.load().collect(Collectors.toList());
        }

        // Aggregate duplicate query strings before inserting
        Map<String, Long> aggregated = new LinkedHashMap<>();
        for (QueryCountRow row : rows) {
            String normalizedQuery = row.queryText().trim().toLowerCase();
            if (!normalizedQuery.isEmpty()) {
                aggregated.merge(normalizedQuery, row.count(), Long::sum);
            }
        }

        log.info("Aggregated to {} unique queries. Performing JDBC batch insert...", aggregated.size());

        Instant now = clock.instant();
        List<Object[]> batchArgs = new ArrayList<>();
        int batchSize = 1000;
        long totalInserted = 0;

        String sql = "INSERT INTO query_count (query_text, total_count, recent_count, last_searched_at, created_at) " +
                "VALUES (?, ?, ?, ?, ?)";

        for (Map.Entry<String, Long> entry : aggregated.entrySet()) {
            batchArgs.add(new Object[] {
                    entry.getKey(),
                    entry.getValue(),
                    0L, // recentCount is initialized to 0
                    null, // lastSearchedAt is null on ingestion
                    now
            });

            if (batchArgs.size() == batchSize) {
                jdbcTemplate.batchUpdate(sql, batchArgs);
                totalInserted += batchArgs.size();
                batchArgs.clear();
            }
        }

        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
            totalInserted += batchArgs.size();
        }

        long endTime = System.currentTimeMillis();
        log.info("Ingestion completed: loaded {} queries in {} ms.", totalInserted, (endTime - startTime));

        // Publish event to trigger index compilation
        publisher.publishEvent(new DatasetLoadedEvent(this));
    }
}
