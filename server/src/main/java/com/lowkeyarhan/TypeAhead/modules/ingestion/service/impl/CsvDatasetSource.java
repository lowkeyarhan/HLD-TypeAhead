package com.lowkeyarhan.TypeAhead.modules.ingestion.service.impl;

import com.lowkeyarhan.TypeAhead.modules.ingestion.dto.QueryCountRow;
import com.lowkeyarhan.TypeAhead.modules.ingestion.service.DatasetSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

// Data source implementation that reads from a classpath CSV dataset.
@Slf4j
public class CsvDatasetSource implements DatasetSource {

    private final String resourcePath;

    public CsvDatasetSource(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public Stream<QueryCountRow> load() {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                log.warn("CSV dataset file not found at classpath:{}", resourcePath);
                return Stream.empty();
            }
            Reader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
            CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build());
            return parser.getRecords().stream()
                    .map(record -> {
                        try {
                            String query = record.get("query");
                            String countStr = record.get("count");
                            if (query == null || query.isBlank()) {
                                log.warn("Skipping CSV row {}: empty query", record.getRecordNumber());
                                return null;
                            }
                            long count = Long.parseLong(countStr);
                            if (count < 0) {
                                log.warn("Skipping CSV row {}: negative count {}", record.getRecordNumber(), count);
                                return null;
                            }
                            return new QueryCountRow(query.trim().toLowerCase(), count);
                        } catch (Exception e) {
                            log.warn("Skipping malformed CSV row {}: {}", record.getRecordNumber(), e.getMessage());
                            return null;
                        }
                    })
                    .filter(java.util.Objects::nonNull)
                    .onClose(() -> {
                        try {
                            parser.close();
                            reader.close();
                        } catch (Exception e) {
                            log.error("Failed to close CSV parser/reader", e);
                        }
                    });
        } catch (Exception e) {
            log.error("Error reading CSV dataset", e);
            return Stream.empty();
        }
    }
}
