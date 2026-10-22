package ru.oleynikova.finance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(
        @Schema(example = "demo@finance.local") String email,
        @Schema(example = "demo12345") String password) {
}
