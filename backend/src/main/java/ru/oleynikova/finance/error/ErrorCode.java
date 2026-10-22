package ru.oleynikova.finance.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST),
    INVALID_JSON(HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    USER_BLOCKED(HttpStatus.FORBIDDEN),

    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND),
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND),
    AUTOPAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    NOT_FOUND(HttpStatus.NOT_FOUND),

    EMAIL_TAKEN(HttpStatus.CONFLICT),

    SAME_ACCOUNT(HttpStatus.UNPROCESSABLE_ENTITY),
    INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_ENTITY),
    DAILY_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY),
    CARD_BLOCKED(HttpStatus.UNPROCESSABLE_ENTITY),
    CARD_EXPIRED(HttpStatus.UNPROCESSABLE_ENTITY),
    CANNOT_BLOCK_ADMIN(HttpStatus.UNPROCESSABLE_ENTITY),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
