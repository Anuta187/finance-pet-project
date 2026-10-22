package ru.oleynikova.finance.autotests.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Card(Long id, Long accountId, String accountName, String maskedNumber, String last4,
                   String paymentSystem, String holderName, int expiryMonth, int expiryYear, String expiry,
                   String status, String createdAt) {
}
