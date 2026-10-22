package ru.oleynikova.finance.autotests.api.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminUser(Long id, String email, String fullName, String role, boolean blocked, String createdAt,
                        long accountsCount, BigDecimal totalBalance) {
}
