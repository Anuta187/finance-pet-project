package ru.oleynikova.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import ru.oleynikova.finance.dto.AuthResponse;
import ru.oleynikova.finance.dto.LoginRequest;
import ru.oleynikova.finance.dto.RegisterRequest;
import ru.oleynikova.finance.entity.Role;
import ru.oleynikova.finance.entity.User;
import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.error.ErrorCode;
import ru.oleynikova.finance.repository.UserRepository;
import ru.oleynikova.finance.security.JwtService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService: регистрация и вход")
class AuthServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T05:00:00Z"), ZoneOffset.UTC);
    // Минимальная стоимость BCrypt, чтобы тесты шли быстрее
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @Mock
    private UserRepository users;

    private AuthService service;

    @BeforeEach
    void setUp() {
        JwtService jwt = new JwtService("unit-test-secret-unit-test-secret-1234", Duration.ofHours(1));
        service = new AuthService(users, encoder, jwt, CLOCK);
    }

    @Test
    @DisplayName("Регистрация: email нормализуется, пароль хранится как BCrypt-хеш, роль CLIENT, выдаётся токен")
    void register() {
        when(users.existsByEmail("anna@example.com")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        AuthResponse res = service.register(new RegisterRequest("  Anna@Example.COM ", "Secret123", " Анна   Иванова "));

        assertThat(res.token()).isNotBlank();
        assertThat(res.tokenType()).isEqualTo("Bearer");
        assertThat(res.user().email()).isEqualTo("anna@example.com");
        assertThat(res.user().fullName()).isEqualTo("Анна Иванова");
        assertThat(res.user().role()).isEqualTo(Role.CLIENT);
    }

    @ParameterizedTest(name = "{0} / {1} / {2}: VALIDATION_ERROR по полю {3}")
    @CsvSource({
            "'', Secret123, Анна, email",
            "not-an-email, Secret123, Анна, email",
            "a@b.ru, Short1, Анна, password",
            "a@b.ru, OnlyLetters, Анна, password",
            "a@b.ru, 12345678, Анна, password",
            "a@b.ru, Secret123, А, fullName"})
    @DisplayName("Регистрация: валидация email, пароля и имени")
    void registerValidation(String email, String password, String fullName, String field) {
        assertThatThrownBy(() -> service.register(new RegisterRequest(email, password, fullName)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    assertThat(((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(((ApiException) e).getDetails()).containsEntry("field", field);
                });
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("Повторная регистрация того же email: EMAIL_TAKEN")
    void emailTaken() {
        when(users.existsByEmail("demo@finance.local")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("demo@finance.local", "Secret123", "Иван")))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.EMAIL_TAKEN);
    }

    @Test
    @DisplayName("Вход с верным паролем возвращает токен; неверный пароль и неизвестный email: INVALID_CREDENTIALS")
    void login() {
        User user = service.createUser("demo@finance.local", "demo12345", "Иван Петров", Role.CLIENT);
        user.setId(2L);
        when(users.findByEmail("demo@finance.local")).thenReturn(Optional.of(user));
        when(users.findByEmail("nobody@finance.local")).thenReturn(Optional.empty());

        assertThat(service.login(new LoginRequest("DEMO@finance.local", "demo12345")).token()).isNotBlank();
        assertThat(user.getPasswordHash()).startsWith("$2a$").doesNotContain("demo12345");
        assertThatThrownBy(() -> service.login(new LoginRequest("demo@finance.local", "wrong-pass1")))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
        assertThatThrownBy(() -> service.login(new LoginRequest("nobody@finance.local", "demo12345")))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("Заблокированный пользователь с верным паролем: USER_BLOCKED")
    void blockedUserCannotLogin() {
        User user = service.createUser("demo@finance.local", "demo12345", "Иван Петров", Role.CLIENT);
        user.setBlocked(true);
        when(users.findByEmail("demo@finance.local")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.login(new LoginRequest("demo@finance.local", "demo12345")))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.USER_BLOCKED);
    }
}
