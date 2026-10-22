package ru.oleynikova.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import ru.oleynikova.finance.entity.OperationType;

// ownerId и ownerEmail заполняются только в админском API
public record OperationDto(Long id, OperationType type, Long fromAccountId, String fromAccountName,
                           Long toAccountId, String toAccountName, BigDecimal amount, String comment,
                           Instant createdAt, Long categoryId, String categoryName, Long cardId, String cardMasked,
                           Long autopaymentId,
                           @JsonInclude(JsonInclude.Include.NON_NULL) Long ownerId,
                           @JsonInclude(JsonInclude.Include.NON_NULL) String ownerEmail) {
}
