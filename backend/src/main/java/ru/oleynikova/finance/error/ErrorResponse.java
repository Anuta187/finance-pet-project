package ru.oleynikova.finance.error;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

public record ErrorResponse(Body error) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Body(String code, String message, Map<String, Object> details) {
    }

    public static ErrorResponse of(ErrorCode code, String message, Map<String, Object> details) {
        return new ErrorResponse(new Body(code.name(), message, details));
    }
}
