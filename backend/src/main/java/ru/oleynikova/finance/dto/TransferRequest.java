package ru.oleynikova.finance.dto;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

// JsonNode, чтобы отличать отсутствующее поле от некорректной суммы
public record TransferRequest(
        @NotNull(message = "Поле «fromAccountId» обязательно")
        @Schema(type = "integer", example = "1")
        JsonNode fromAccountId,

        @NotNull(message = "Поле «toAccountId» обязательно")
        @Schema(type = "integer", example = "2")
        JsonNode toAccountId,

        @NotNull(message = "Поле «amount» обязательно")
        @Schema(type = "number", example = "1500.5")
        JsonNode amount,

        String comment) {
}
