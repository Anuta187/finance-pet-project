package ru.oleynikova.finance.autotests.data;

import ru.oleynikova.finance.autotests.api.model.User;

public record Session(String token, User user) {

    public long userId() {
        return user.id();
    }

    public String email() {
        return user.email();
    }
}
