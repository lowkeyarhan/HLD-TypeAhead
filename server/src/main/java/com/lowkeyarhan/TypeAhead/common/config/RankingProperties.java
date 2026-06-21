package com.lowkeyarhan.TypeAhead.common.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

// Configuration properties for query ranking strategies.
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "typeahead.ranking")
public class RankingProperties {

    @NotNull(message = "decayHalfLifeMinutes must be provided")
    @Min(value = 1, message = "decayHalfLifeMinutes must be at least 1")
    private Double decayHalfLifeMinutes;

    @NotBlank(message = "strategy must be provided")
    private String strategy;
}
