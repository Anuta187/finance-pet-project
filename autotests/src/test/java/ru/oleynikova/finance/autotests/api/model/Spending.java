package ru.oleynikova.finance.autotests.api.model;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Spending(String month, BigDecimal total, List<Item> categories) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(Long categoryId, String code, String name, String color, BigDecimal amount, long count) {
    }
}
