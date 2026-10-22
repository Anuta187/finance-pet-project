package ru.oleynikova.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Поля проверяются в AuthService
public record RegisterRequest(
        @Schema(example = "anna@example.com") String email,
        @Schema(example = "Secret123") String password,
        @Schema(example = "Анна Иванова") String fullName) {
}
