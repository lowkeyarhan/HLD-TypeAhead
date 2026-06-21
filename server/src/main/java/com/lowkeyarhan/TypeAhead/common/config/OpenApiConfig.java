package com.lowkeyarhan.TypeAhead.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// OpenAPI/Swagger documentation metadata configuration.
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI typeAheadOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("TypeAhead API")
                        .description("High-Level Design: distributed search typeahead with consistent hashing, recency-aware ranking, and batch write buffering")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("lowkeyarhan")
                                .url("https://github.com/lowkeyarhan/HLD-TypeAhead"))
                        .license(new License().name("MIT")));
    }
}
