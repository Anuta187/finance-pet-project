package ru.oleynikova.finance.autotests.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

// Поля Object, чтобы в негативных тестах отправлять что угодно
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransferRequest(Object fromAccountId, Object toAccountId, Object amount, String comment) {

    public static TransferRequest of(Object fromAccountId, Object toAccountId, Object amount) {
        return new TransferRequest(fromAccountId, toAccountId, amount, null);
    }
}
