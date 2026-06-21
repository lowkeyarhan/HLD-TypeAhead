package com.lowkeyarhan.TypeAhead.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Configuration to expose a consistent, injectable time source.
// This enables deterministic testing of time-based features (e.g. recency decay ranking).
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
