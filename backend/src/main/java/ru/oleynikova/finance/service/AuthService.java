package ru.oleynikova.finance.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.oleynikova.finance.dto.AuthResponse;
import ru.oleynikova.finance.dto.LoginRequest;
import ru.oleynikova.finance.dto.RegisterRequest;
import ru.oleynikova.finance.dto.UserDto;
import ru.oleynikova.finance.entity.Role;
import ru.oleynikova.finance.entity.User;
import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.error.ErrorCode;
import ru.oleynikova.finance.repository.UserRepository;
import ru.oleynikova.finance.security.JwtService;

@Service
public class AuthService {

    static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    static final Pattern HAS_LETTER = Pattern.compile("\\p{L}");
    static final Pattern HAS_DIGIT = Pattern.compile("\\d");

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request == null ? null : request.email());
        if (email.isEmpty()) {
            throw ApiException.validation("email", "Email обязателен");
        }
        if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            throw ApiException.validation("email", "Некорректный email");
        }
        String password = request.password() == null ? "" : request.password();
        if (password.length() < 8 || password.length() > 72) {
            throw ApiException.validation("password", "Пароль должен быть от 8 до 72 символов");
        }
        if (!HAS_LETTER.matcher(password).find() || !HAS_DIGIT.matcher(password).find()) {
            throw ApiException.validation("password", "Пароль должен содержать хотя бы одну букву и одну цифру");
        }
        String fullName = request.fullName() == null ? "" : request.fullName().trim().replaceAll("\\s+", " ");
        if (fullName.length() < 2 || fullName.length() > 100) {
            throw ApiException.validation("fullName", "Имя должно быть от 2 до 100 символов");
        }
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_TAKEN, "Пользователь с таким email уже зарегистрирован");
        }
        User user = users.save(createUser(email, password, fullName, Role.CLIENT));
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request == null ? null : request.email());
        String password = request == null || request.password() == null ? "" : request.password();
        if (email.isEmpty()) {
            throw ApiException.validation("email", "Email обязателен");
        }
        if (password.isEmpty()) {
            throw ApiException.validation("password", "Пароль обязателен");
        }
        // Одинаковый ответ, чтобы нельзя было узнать, зарегистрирован ли email
        User user = users.findByEmail(email)
                .filter(u -> encoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS, "Неверный email или пароль"));
        if (user.isBlocked()) {
            throw ApiException.userBlocked();
        }
        return tokenFor(user);
    }

    @Transactional(readOnly = true)
    public UserDto me(long userId) {
        return users.findById(userId).map(UserDto::of).orElseThrow(() -> ApiException.userNotFound(userId));
    }

    public User createUser(String email, String rawPassword, String fullName, Role role) {
        return new User(normalizeEmail(email), encoder.encode(rawPassword), fullName, role,
                Instant.now(clock).truncatedTo(ChronoUnit.MILLIS));
    }

    private AuthResponse tokenFor(User user) {
        JwtService.IssuedToken t = jwt.issue(user);
        return new AuthResponse(t.token(), "Bearer", t.expiresAt(), UserDto.of(user));
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
