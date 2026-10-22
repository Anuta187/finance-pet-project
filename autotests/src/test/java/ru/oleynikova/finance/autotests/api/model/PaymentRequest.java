package ru.oleynikova.finance.autotests.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentRequest(Object categoryId, Object accountId, Object cardId, Object amount, String comment) {

    public static PaymentRequest fromAccount(long categoryId, long accountId, Object amount) {
        return new PaymentRequest(categoryId, accountId, null, amount, null);
    }

    public static PaymentRequest byCard(long categoryId, long cardId, Object amount) {
        return new PaymentRequest(categoryId, null, cardId, amount, null);
    }
}
