package ru.oleynikova.finance.autotests.api.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Operation(Long id, String type, Long fromAccountId, String fromAccountName, Long toAccountId,
                        String toAccountName, BigDecimal amount, String comment, String createdAt,
                        Long categoryId, String categoryName, Long cardId, String cardMasked, Long autopaymentId,
                        Long ownerId, String ownerEmail) {
}
