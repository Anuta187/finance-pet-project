package ru.oleynikova.finance.autotests.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentResult(Operation operation, Account account) {
}
