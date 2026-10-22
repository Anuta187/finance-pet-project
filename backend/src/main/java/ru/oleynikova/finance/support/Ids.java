package ru.oleynikova.finance.support;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;

import ru.oleynikova.finance.error.ApiException;

public final class Ids {

    private Ids() {
    }

    public static long parse(JsonNode value, String field) {
        if (value == null || value.isNull() || value.isMissingNode()) {
            throw required(field);
        }
        if (value.isNumber()) {
            return parse(value.decimalValue(), field);
        }
        if (value.isTextual()) {
            return parse(value.asText(), field);
        }
        throw invalid(field);
    }

    public static Long parseOptional(JsonNode value, String field) {
        if (value == null || value.isNull() || value.isMissingNode()
                || (value.isTextual() && value.asText().isBlank())) {
            return null;
        }
        return parse(value, field);
    }

    public static long parse(String value, String field) {
        if (value == null || value.isEmpty()) {
            throw required(field);
        }
        try {
            return parse(new BigDecimal(value.trim()), field);
        } catch (NumberFormatException e) {
            throw invalid(field);
        }
    }

    private static long parse(BigDecimal n, String field) {
        if (n.signum() <= 0 || n.stripTrailingZeros().scale() > 0 || n.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            throw invalid(field);
        }
        return n.longValueExact();
    }

    private static ApiException required(String field) {
        return ApiException.validation(field, "Поле «" + field + "» обязательно");
    }

    private static ApiException invalid(String field) {
        return ApiException.validation(field, "Поле «" + field + "» должно быть положительным целым числом");
    }
}
