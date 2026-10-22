package ru.oleynikova.finance.dto;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

public record IssueCardRequest(
        @Schema(type = "integer", example = "1") JsonNode accountId,
        @Schema(example = "IVAN PETROV") String holderName) {
}
