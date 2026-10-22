package ru.oleynikova.finance.dto;

import java.time.Instant;

public record AuthResponse(String token, String tokenType, Instant expiresAt, UserDto user) {
}
