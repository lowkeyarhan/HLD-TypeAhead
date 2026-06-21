package com.lowkeyarhan.TypeAhead.modules.index;

import com.lowkeyarhan.TypeAhead.common.metrics.MetricsService;
import com.lowkeyarhan.TypeAhead.modules.data.QueryCount;
import com.lowkeyarhan.TypeAhead.modules.data.QueryCountRepository;
import com.lowkeyarhan.TypeAhead.modules.ingestion.DatasetLoadedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

// Thread-safe in-memory index for fast prefix lookups.
// Implementation trade-off: A TreeMap is used here instead of a Trie.
// TreeMap is simpler to implement correctly, and for 100k scale, the subMap() range
// scan is extremely fast (under 10ms). A Trie would be preferred at a much larger scale.
// Thread-safety is achieved using copy-on-write reference swapping for lock-free reads.
@Slf4j
@Service
public class PrefixIndexServiceImpl implements PrefixIndexService {

    private final QueryCountRepository repository;
    private final MetricsService metricsService;
    private volatile TreeMap<String, QueryCount> index = new TreeMap<>();

    public PrefixIndexServiceImpl(QueryCountRepository repository, MetricsService metricsService) {
        this.repository = repository;
        this.metricsService = metricsService;
    }

    @Override
    public List<QueryCount> search(String prefix, int limit) {
        if (prefix == null) {
            return List.of();
        }
        String normalized = prefix.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return List.of();
        }
        // Capture reference once for safe lock-free reading
        TreeMap<String, QueryCount> current = this.index;

        // subMap(from, to) creates a sorted view of matching prefixes
        return current.subMap(normalized, normalized + Character.MAX_VALUE).values().stream()
                .sorted(Comparator.comparing(QueryCount::getTotalCount).reversed()
                        .thenComparing(QueryCount::getQueryText))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized void applyDelta(String queryText, long countDelta, Instant searchedAt) {
        // Copy-on-write mapping
        TreeMap<String, QueryCount> newIndex = new TreeMap<>(this.index);
        QueryCount qc = newIndex.get(queryText);
        if (qc == null) {
            qc = QueryCount.builder()
                    .queryText(queryText)
                    .totalCount(0L)
                    .recentCount(0L)
                    .createdAt(searchedAt)
                    .build();
        }
        qc.setTotalCount(qc.getTotalCount() + countDelta);
        qc.setRecentCount(qc.getRecentCount() + countDelta);
        qc.setLastSearchedAt(searchedAt);
        newIndex.put(queryText, qc);
        this.index = newIndex;
    }

    @Override
    public synchronized void rebuild() {
        log.info("Rebuilding in-memory prefix index...");
        long startTime = System.currentTimeMillis();
        metricsService.incrementDbReads(1);
        List<QueryCount> allQueries = repository.findAll();
        TreeMap<String, QueryCount> newIndex = new TreeMap<>();
        for (QueryCount qc : allQueries) {
            newIndex.put(qc.getQueryText(), qc);
        }
        this.index = newIndex;
        long endTime = System.currentTimeMillis();
        log.info("Prefix index rebuilt with {} queries in {} ms.", newIndex.size(), (endTime - startTime));
    }

    // Observer Pattern: listens for dataset ingestion completeness to trigger
    // rebuild
    @EventListener
    public void onDatasetLoaded(DatasetLoadedEvent event) {
        rebuild();
    }

    @Override
    public List<QueryCount> getAllCandidates() {
        return new java.util.ArrayList<>(this.index.values());
    }
}
