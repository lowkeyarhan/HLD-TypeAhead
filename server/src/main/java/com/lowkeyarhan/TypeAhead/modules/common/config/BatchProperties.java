package com.lowkeyarhan.TypeAhead.modules.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

// Configuration properties for the write buffering and batch flushing engine.
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "typeahead.batch")
public class BatchProperties {

    @NotNull(message = "flushIntervalMs must be provided")
    @Min(value = 1, message = "flushIntervalMs must be at least 1")
    private Long flushIntervalMs;

    @NotNull(message = "sizeThreshold must be provided")
    @Min(value = 1, message = "sizeThreshold must be at least 1")
    private Integer sizeThreshold;
}
