package ru.oleynikova.finance.autotests.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Category(Long id, String code, String name, String color) {
}
