package com.lowkeyarhan.TypeAhead.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the distributed cache layer.
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "typeahead.cache")
public class CacheProperties {

    @NotNull(message = "nodeCount must be provided")
    @Min(value = 1, message = "nodeCount must be at least 1")
    private Integer nodeCount;

    @NotNull(message = "virtualNodes must be provided")
    @Min(value = 1, message = "virtualNodes must be at least 1")
    private Integer virtualNodes;

    @NotNull(message = "ttlSeconds must be provided")
    @Min(value = 1, message = "ttlSeconds must be at least 1")
    private Long ttlSeconds;
}
