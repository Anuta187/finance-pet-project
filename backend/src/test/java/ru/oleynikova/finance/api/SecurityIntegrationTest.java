package ru.oleynikova.finance.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@DisplayName("Безопасность: JWT, роли, изоляция данных, блокировка")
class SecurityIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("Без токена, с испорченным токеном и с Basic вместо Bearer ответ 401")
    void unauthorized() throws Exception {
        mvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(as(demo + "x", get("/api/accounts")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mvc.perform(get("/api/accounts").header(HttpHeaders.AUTHORIZATION, "Basic ZGVtbzpkZW1v"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Регистрация: токен работает, /api/auth/me возвращает профиль с ролью CLIENT")
    void registerAndMe() throws Exception {
        String token = register("new.user@test.ru");

        mvc.perform(as(token, get("/api/auth/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.user@test.ru"))
                .andExpect(jsonPath("$.role").value("CLIENT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(as(token, get("/api/accounts"))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "NEW.user@test.ru", "password", "Secret123", "fullName", "Дубль")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_TAKEN"));
    }

    @Test
    @DisplayName("Неверный пароль: 401 INVALID_CREDENTIALS")
    void invalidCredentials() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "demo@finance.local", "password", "wrong")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Клиент не видит чужие счета: список пуст, счёт по id и перевод с чужого счёта: 404")
    void dataIsolation() throws Exception {
        String other = register("other@test.ru");

        mvc.perform(as(other, get("/api/accounts/1")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"));
        mvc.perform(as(other, get("/api/operations")).param("accountId", "1"))
                .andExpect(status().isNotFound());
        mvc.perform(as(other, get("/api/operations"))).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(jsonAs(other, post("/api/transfers"), "{\"fromAccountId\":1,\"toAccountId\":2,\"amount\":1}"))
                .andExpect(status().isNotFound());
        mvc.perform(jsonAs(other, post("/api/payments"), "{\"categoryId\":1,\"cardId\":1,\"amount\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CARD_NOT_FOUND"));
        mvc.perform(as(demo, get("/api/accounts/1"))).andExpect(jsonPath("$.balance").value(37540.5));
    }

    @Test
    @DisplayName("Админ-API: клиенту 403, администратору 200")
    void roles() throws Exception {
        mvc.perform(as(demo, get("/api/admin/users")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mvc.perform(as(demo, post("/api/admin/autopayments/run"))).andExpect(status().isForbidden());
        mvc.perform(as(admin, get("/api/admin/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].email").value("demo@finance.local"))
                .andExpect(jsonPath("$[1].accountsCount").value(3));
    }

    @Test
    @DisplayName("Заблокированный пользователь получает USER_BLOCKED, после разблокировки всё работает")
    void blockUser() throws Exception {
        mvc.perform(as(admin, post("/api/admin/users/2/block")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blocked").value(true));

        mvc.perform(as(demo, get("/api/accounts")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_BLOCKED"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "demo@finance.local", "password", "demo12345")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_BLOCKED"));

        mvc.perform(as(admin, post("/api/admin/users/2/unblock"))).andExpect(jsonPath("$.blocked").value(false));
        mvc.perform(as(demo, get("/api/accounts"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Администратора заблокировать нельзя: 422 CANNOT_BLOCK_ADMIN; неизвестный пользователь: 404")
    void cannotBlockAdmin() throws Exception {
        mvc.perform(as(admin, post("/api/admin/users/1/block")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("CANNOT_BLOCK_ADMIN"));
        mvc.perform(as(admin, post("/api/admin/users/999/block")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }
}
