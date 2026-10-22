package ru.oleynikova.finance.dto;

import java.math.BigDecimal;
import java.time.Instant;

import ru.oleynikova.finance.entity.Role;

public record AdminUserDto(Long id, String email, String fullName, Role role, boolean blocked, Instant createdAt,
                           long accountsCount, BigDecimal totalBalance) {
}
