package ru.oleynikova.finance.security;

import ru.oleynikova.finance.entity.Role;

public record AuthUser(long id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
