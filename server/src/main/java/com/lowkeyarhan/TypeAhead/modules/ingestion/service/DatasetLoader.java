package com.lowkeyarhan.TypeAhead.modules.ingestion.service;

import com.lowkeyarhan.TypeAhead.common.metrics.MetricsService;
import com.lowkeyarhan.TypeAhead.modules.data.repository.QueryCountRepository;
import com.lowkeyarhan.TypeAhead.modules.ingestion.DatasetLoadedEvent;
import com.lowkeyarhan.TypeAhead.modules.ingestion.dto.QueryCountRow;
import com.lowkeyarhan.TypeAhead.modules.ingestion.service.impl.CsvDatasetSource;
import com.lowkeyarhan.TypeAhead.modules.ingestion.service.impl.SyntheticDatasetSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
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

// ApplicationRunner that loads query count data on startup, idempotently.
@Slf4j
@Component
public class DatasetLoader implements ApplicationRunner {

    private final QueryCountRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher publisher;
    private final MetricsService metricsService;
    private final Clock clock;

    public DatasetLoader(QueryCountRepository repository,
            JdbcTemplate jdbcTemplate,
            ApplicationEventPublisher publisher,
            MetricsService metricsService,
            Clock clock) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.publisher = publisher;
        this.metricsService = metricsService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        metricsService.incrementDbReads(1);
        long countInDb = repository.count();
        if (countInDb > 0) {
            log.info("Database already contains {} queries. Skipping ingestion.", countInDb);
            publisher.publishEvent(new DatasetLoadedEvent(this));
            return;
        }
        log.info("Starting dataset ingestion...");
        long startTime = System.currentTimeMillis();
        DatasetSource source = new CsvDatasetSource("dataset/queries.csv");
        Stream<QueryCountRow> stream = source.load();
        List<QueryCountRow> rows = stream.collect(Collectors.toList());
        if (rows.isEmpty()) {
            log.info("Falling back to SyntheticDatasetSource for generating 100,000+ queries.");
            source = new SyntheticDatasetSource(100000);
            rows = source.load().collect(Collectors.toList());
        }
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
            batchArgs.add(new Object[] { entry.getKey(), entry.getValue(), 0L, null, java.sql.Timestamp.from(now) });
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
        metricsService.incrementDbWrites(totalInserted);
        log.info("Ingestion completed: loaded {} queries in {} ms.", totalInserted,
                System.currentTimeMillis() - startTime);
        publisher.publishEvent(new DatasetLoadedEvent(this));
    }
}
