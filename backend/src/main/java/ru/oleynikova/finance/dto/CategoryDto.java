package ru.oleynikova.finance.dto;

import ru.oleynikova.finance.entity.Category;

public record CategoryDto(Long id, String code, String name, String color) {

    public static CategoryDto of(Category c) {
        return new CategoryDto(c.getId(), c.getCode(), c.getName(), c.getColor());
    }
}
