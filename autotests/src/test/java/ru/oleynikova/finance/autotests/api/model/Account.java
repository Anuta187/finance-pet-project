package ru.oleynikova.finance.autotests.api.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Account(Long id, String name, String currency, BigDecimal balance, String createdAt) {
}
