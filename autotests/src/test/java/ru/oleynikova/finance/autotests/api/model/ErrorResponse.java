package ru.oleynikova.finance.autotests.api.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorResponse(ApiError error) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiError(String code, String message, Map<String, Object> details) {
    }
}
