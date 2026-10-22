package ru.oleynikova.finance.autotests.api.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Autopayment(long id, String name, Long accountId, String accountName, Long cardId, String cardMasked,
                          Long categoryId, String categoryName, BigDecimal amount, String period, String startDate,
                          String nextRunDate, boolean active, String lastRunAt, String lastStatus, String lastError,
                          String createdAt) {
}
