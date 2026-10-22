package ru.oleynikova.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import ru.oleynikova.finance.entity.AutopaymentPeriod;
import ru.oleynikova.finance.entity.AutopaymentStatus;

public record AutopaymentDto(Long id, String name, Long accountId, String accountName, Long cardId, String cardMasked,
                             Long categoryId, String categoryName, BigDecimal amount, AutopaymentPeriod period,
                             LocalDate startDate, LocalDate nextRunDate, boolean active, Instant lastRunAt,
                             AutopaymentStatus lastStatus, String lastError, Instant createdAt) {
}
