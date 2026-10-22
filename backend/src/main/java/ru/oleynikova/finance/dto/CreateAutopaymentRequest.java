package ru.oleynikova.finance.dto;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

// Если startDate не указан, первое списание сегодня
public record CreateAutopaymentRequest(
        @Schema(example = "Мобильная связь") String name,
        @Schema(type = "integer", example = "1") JsonNode categoryId,
        @Schema(type = "integer", example = "1") JsonNode accountId,
        @Schema(type = "integer", example = "1") JsonNode cardId,
        @Schema(type = "number", example = "650") JsonNode amount,
        @Schema(example = "MONTHLY", allowableValues = {"DAILY", "WEEKLY", "MONTHLY"}) String period,
        @Schema(example = "2026-11-01") String startDate) {
}
