package ru.oleynikova.finance.error;

import java.util.Map;

public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final transient Map<String, Object> details;

    public ApiException(ErrorCode code, String message) {
        this(code, message, null);
    }

    public ApiException(ErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public ErrorCode getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public static ApiException validation(String field, String message) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message, Map.of("field", field));
    }

    // Чужой счёт тоже "не найден"
    public static ApiException accountNotFound(long id) {
        return new ApiException(ErrorCode.ACCOUNT_NOT_FOUND, "Счёт с id=" + id + " не найден", Map.of("accountId", id));
    }

    public static ApiException cardNotFound(long id) {
        return new ApiException(ErrorCode.CARD_NOT_FOUND, "Карта с id=" + id + " не найдена", Map.of("cardId", id));
    }

    public static ApiException categoryNotFound(long id) {
        return new ApiException(ErrorCode.CATEGORY_NOT_FOUND, "Категория с id=" + id + " не найдена",
                Map.of("categoryId", id));
    }

    public static ApiException autopaymentNotFound(long id) {
        return new ApiException(ErrorCode.AUTOPAYMENT_NOT_FOUND, "Автоплатёж с id=" + id + " не найден",
                Map.of("autopaymentId", id));
    }

    public static ApiException userNotFound(long id) {
        return new ApiException(ErrorCode.USER_NOT_FOUND, "Пользователь с id=" + id + " не найден", Map.of("userId", id));
    }

    public static ApiException userBlocked() {
        return new ApiException(ErrorCode.USER_BLOCKED, "Пользователь заблокирован. Обратитесь в поддержку банка");
    }
}
