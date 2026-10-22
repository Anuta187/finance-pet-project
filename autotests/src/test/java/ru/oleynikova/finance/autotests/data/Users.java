package ru.oleynikova.finance.autotests.data;

import io.qameta.allure.Allure;

import ru.oleynikova.finance.autotests.api.client.AuthClient;
import ru.oleynikova.finance.autotests.api.model.AuthResponse;

public final class Users {

    public static final String DEMO_EMAIL = "demo@finance.local";
    public static final String DEMO_PASSWORD = "demo12345";
    public static final String ADMIN_EMAIL = "admin@finance.local";
    public static final String ADMIN_PASSWORD = "admin12345";
    public static final String DEFAULT_PASSWORD = "Passw0rd123";

    private static final AuthClient AUTH = new AuthClient();

    private Users() {
    }

    public static Session login(String email, String password) {
        return Allure.step("Войти как " + email, () -> {
            AuthResponse res = AUTH.login(email, password);
            return new Session(res.token(), res.user());
        });
    }

    public static Session demo() {
        return login(DEMO_EMAIL, DEMO_PASSWORD);
    }

    public static Session admin() {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    // для тестов, которые меняют состояние пользователя
    public static Session newClient() {
        String email = TestData.uniqueEmail();
        return Allure.step("Зарегистрировать клиента " + email, () -> {
            AuthResponse res = AUTH.register(email, DEFAULT_PASSWORD, "Тест Клиентов");
            return new Session(res.token(), res.user());
        });
    }
}
