package ru.oleynikova.finance.dto;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

public record PaymentRequest(
        @Schema(type = "integer", example = "1") JsonNode categoryId,
        @Schema(type = "integer", example = "1") JsonNode accountId,
        @Schema(type = "integer", example = "1") JsonNode cardId,
        @Schema(type = "number", example = "650") JsonNode amount,
        @Schema(example = "Номер телефона 79140000000") String comment) {
}
