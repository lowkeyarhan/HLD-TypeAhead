package com.lowkeyarhan.TypeAhead.modules.ingestion;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// Data source implementation generating Zipfian-distributed mock queries.
public class SyntheticDatasetSource implements DatasetSource {

    private final int totalQueries;

    public SyntheticDatasetSource(int totalQueries) {
        this.totalQueries = totalQueries;
    }

    @Override
    public Stream<QueryCountRow> load() {
        String[] words = {
                "iphone", "java", "spring", "docker", "postgres", "redis", "react", "nextjs",
                "angular", "vue", "html", "css", "javascript", "typescript", "python", "rust",
                "golang", "tutorial", "course", "guide", "example", "api", "framework", "library",
                "database", "cache", "consistent", "hashing", "ring", "node", "cluster", "scaling",
                "performance", "latency", "throughput", "concurrency", "thread", "lock", "asynchronous",
                "reactive", "monolith", "microservice", "dockerfile", "compose", "git", "github",
                "branch", "commit", "push", "pull", "merge", "rebase", "conflict", "repository",
                "controller", "service", "model", "dto", "validation", "exception", "handler",
                "metrics", "latency", "percentile", "p95", "p99", "prometheus", "grafana", "actuator"
        };

        Random random = new Random(42);
        Set<String> uniqueQueries = new LinkedHashSet<>();

        // Generate unique query strings
        while (uniqueQueries.size() < totalQueries) {
            int numWords = 1 + random.nextInt(3); // 1 to 3 words
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < numWords; i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(words[random.nextInt(words.length)]);
            }
            uniqueQueries.add(sb.toString().trim().toLowerCase());
        }

        List<String> queryList = new ArrayList<>(uniqueQueries);

        // Produce Zipfian-ish counts: count = C / (rank ^ alpha)
        return IntStream.range(0, totalQueries)
                .mapToObj(i -> {
                    String query = queryList.get(i);
                    long count = Math.max(1L, Math.round(500000.0 / Math.pow(i + 1, 0.75)));
                    return new QueryCountRow(query, count);
                });
    }
}
