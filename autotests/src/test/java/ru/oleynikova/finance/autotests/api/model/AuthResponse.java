package ru.oleynikova.finance.autotests.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthResponse(String token, String tokenType, String expiresAt, User user) {
}
