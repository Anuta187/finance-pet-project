package ru.oleynikova.finance.dto;

import java.math.BigDecimal;
import java.util.List;

public record SpendingDto(String month, BigDecimal total, List<Item> categories) {

    public record Item(Long categoryId, String code, String name, String color, BigDecimal amount, long count) {
    }
}
