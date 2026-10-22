package ru.oleynikova.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountDto(Long id, String name, String currency, BigDecimal balance, Instant createdAt) {
}
