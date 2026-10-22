package ru.oleynikova.finance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

// Swagger показывает openapi.yaml, а сгенерированная спецификация нужна для теста
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI generatedOpenApi() {
        return new OpenAPI().info(new Info().title("Личные финансы — REST API (сгенерировано по коду)").version("1.0.0"));
    }
}
