package ru.oleynikova.finance.autotests.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record User(Long id, String email, String fullName, String role, boolean blocked, String createdAt) {
}
