package ru.oleynikova.finance.dto;

import java.time.Instant;

import ru.oleynikova.finance.entity.Role;
import ru.oleynikova.finance.entity.User;

public record UserDto(Long id, String email, String fullName, Role role, boolean blocked, Instant createdAt) {

    public static UserDto of(User u) {
        return new UserDto(u.getId(), u.getEmail(), u.getFullName(), u.getRole(), u.isBlocked(), u.getCreatedAt());
    }
}
