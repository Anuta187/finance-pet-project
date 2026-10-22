package ru.oleynikova.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateAccountRequest(
        @NotBlank(message = "Название счёта обязательно")
        @Size(max = 50, message = "Название счёта — не более 50 символов")
        String name,

        @Schema(type = "number", example = "1000")
        JsonNode initialBalance) {
}
