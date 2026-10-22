package ru.oleynikova.finance.support;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.error.ErrorCode;

/** Внутри суммы хранятся в копейках, наружу отдаются в рублях. */
public final class Money {

    /** Максимум для одной операции: 1 млрд ₽. */
    public static final long MAX_AMOUNT_KOP = 1_000_000_000L * 100;

    private static final Pattern AMOUNT = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    private Money() {
    }

    /** Сумма из JSON: число 1500.5 или строка "1500,50". */
    public static long parseAmount(JsonNode value, String field, boolean allowZero) {
        if (value == null || value.isNull() || value.isMissingNode() || (value.isTextual() && value.asText().isEmpty())) {
            throw ApiException.validation(field, "Поле «" + field + "» обязательно");
        }
        String str;
        if (value.isNumber()) {
            str = value.isIntegralNumber()
                    ? value.bigIntegerValue().toString()
                    : value.decimalValue().stripTrailingZeros().toPlainString();
        } else if (value.isTextual()) {
            str = value.asText().trim().replace(',', '.');
        } else {
            throw invalid(field, "Сумма должна быть числом");
        }
        return parseAmount(str, field, allowZero);
    }

    public static long parseAmount(String str, String field, boolean allowZero) {
        if (!AMOUNT.matcher(str).matches()) {
            throw invalid(field, "Сумма должна быть положительным числом, не более 2 знаков после запятой");
        }
        BigDecimal kop = new BigDecimal(str).movePointRight(2);
        if (!allowZero && kop.signum() <= 0) {
            throw invalid(field, "Сумма должна быть больше 0");
        }
        if (kop.compareTo(BigDecimal.valueOf(MAX_AMOUNT_KOP)) > 0) {
            throw invalid(field, "Сумма превышает допустимый максимум");
        }
        return kop.longValueExact();
    }

    public static BigDecimal toRub(long kop) {
        BigDecimal rub = BigDecimal.valueOf(kop, 2).stripTrailingZeros();
        return rub.scale() < 0 ? rub.setScale(0) : rub;
    }

    private static ApiException invalid(String field, String message) {
        return new ApiException(ErrorCode.INVALID_AMOUNT, message, Map.of("field", field));
    }
}
