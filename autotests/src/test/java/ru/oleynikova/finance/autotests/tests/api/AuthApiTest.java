package ru.oleynikova.finance.autotests.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.oleynikova.finance.autotests.assertions.ApiAssertions.assertError;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import ru.oleynikova.finance.autotests.api.client.AuthClient;
import ru.oleynikova.finance.autotests.api.model.AuthResponse;
import ru.oleynikova.finance.autotests.api.model.User;
import ru.oleynikova.finance.autotests.data.TestData;
import ru.oleynikova.finance.autotests.data.Users;
import ru.oleynikova.finance.autotests.tests.BaseTest;

@Tag("api")
@Epic("Личные финансы")
@Feature("API: регистрация и вход (JWT)")
@DisplayName("API: регистрация и вход")
class AuthApiTest extends BaseTest {

    private final AuthClient auth = new AuthClient();

    @Test
    @Tag("smoke")
    @Story("US-08 Регистрация")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("AT-27 Регистрация: 201, JWT и роль CLIENT; /api/auth/me возвращает того же пользователя")
    void registerAndMe() {
        String email = TestData.uniqueEmail();

        AuthResponse res = auth.register(email, "Secret123", "  Анна   Олейникова ");

        assertThat(res.token()).isNotBlank().contains(".");
        assertThat(res.tokenType()).isEqualTo("Bearer");
        assertThat(res.expiresAt()).isNotBlank();
        assertThat(res.user().email()).isEqualTo(email);
        assertThat(res.user().fullName()).isEqualTo("Анна Олейникова");
        assertThat(res.user().role()).isEqualTo("CLIENT");
        assertThat(res.user().blocked()).isFalse();

        User me = new AuthClient(res.token()).me();
        assertThat(me.id()).isEqualTo(res.user().id());
        assertThat(me.email()).isEqualTo(email);
    }

    @Test
    @Story("US-08 Регистрация")
    @DisplayName("AT-28 Повторная регистрация email в другом регистре: 409 EMAIL_TAKEN")
    void duplicateEmail() {
        String email = TestData.uniqueEmail();
        auth.register(email, "Secret123", "Первый");

        assertError(auth.registerRaw(Map.of("email", email.toUpperCase(), "password", "Secret123",
                "fullName", "Второй")), 409, "EMAIL_TAKEN");
        assertError(auth.registerRaw(Map.of("email", Users.DEMO_EMAIL, "password", "Secret123",
                "fullName", "Самозванец")), 409, "EMAIL_TAKEN");
    }

    static Stream<Arguments> invalidRegistrations() {
        return Stream.of(
                Arguments.of("email без @", "not-an-email", "Secret123", "Имя"),
                Arguments.of("пустой email", "", "Secret123", "Имя"),
                Arguments.of("пароль 7 символов (граница)", null, "Secr123", "Имя"),
                Arguments.of("пароль без цифр", null, "SecretSecret", "Имя"),
                Arguments.of("пароль без букв", null, "1234567890", "Имя"),
                Arguments.of("пароль 73 символа (граница)", null, "a1".repeat(36) + "a", "Имя"),
                Arguments.of("имя из 1 символа", null, "Secret123", "Я"),
                Arguments.of("без имени", null, "Secret123", null));
    }

    @ParameterizedTest(name = "AT-29 Валидация регистрации: {0} — 400 VALIDATION_ERROR")
    @MethodSource("invalidRegistrations")
    @Story("US-08 Регистрация")
    @DisplayName("AT-29 Валидация регистрации")
    void invalidRegistration(String title, String email, String password, String fullName) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", email == null ? TestData.uniqueEmail() : email);
        body.put("password", password);
        if (fullName != null) {
            body.put("fullName", fullName);
        }
        assertError(auth.registerRaw(body), 400, "VALIDATION_ERROR");
    }

    @Test
    @Story("US-08 Регистрация")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-30 Поле role при регистрации игнорируется")
    void cannotRegisterAsAdmin() {
        Map<String, Object> body = Map.of("email", TestData.uniqueEmail(), "password", "Secret123",
                "fullName", "Хакер", "role", "ADMIN");

        AuthResponse res = auth.registerRaw(body).then().statusCode(201).extract().as(AuthResponse.class);

        assertThat(res.user().role()).isEqualTo("CLIENT");
    }

    @Test
    @Tag("smoke")
    @Story("US-09 Вход")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("AT-31 Вход демо-клиента и администратора возвращает токен с правильной ролью")
    void loginSeedUsers() {
        AuthResponse client = auth.login(" DEMO@finance.local ", Users.DEMO_PASSWORD);
        AuthResponse admin = auth.login(Users.ADMIN_EMAIL, Users.ADMIN_PASSWORD);

        assertThat(client.user().role()).isEqualTo("CLIENT");
        assertThat(client.user().fullName()).isEqualTo("Иван Петров");
        assertThat(admin.user().role()).isEqualTo("ADMIN");
        assertThat(new AuthClient(admin.token()).me().email()).isEqualTo(Users.ADMIN_EMAIL);
    }

    @Test
    @Story("US-09 Вход")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-32 Неверный пароль и несуществующий email дают одинаковый 401")
    void invalidCredentials() {
        var wrongPassword = assertError(auth.loginRaw(Users.DEMO_EMAIL, "wrong-pass1"), 401, "INVALID_CREDENTIALS");
        var unknownEmail = assertError(auth.loginRaw("nobody@finance.local", "wrong-pass1"), 401, "INVALID_CREDENTIALS");

        // одинаковое сообщение — по ответу нельзя узнать, зарегистрирован ли email
        assertThat(wrongPassword.message()).isEqualTo(unknownEmail.message());
        // пустое тело — ошибка валидации, а не попытка входа
        assertError(auth.loginRaw(Map.of()), 400, "VALIDATION_ERROR");
    }
}
