package ru.oleynikova.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountDetailsDto(Long id, String name, String currency, BigDecimal balance, Instant createdAt,
                                BigDecimal dailyLimit, BigDecimal dailyTransferred, BigDecimal dailyLimitRemaining) {
}
