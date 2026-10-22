package ru.oleynikova.finance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.oleynikova.finance.dto.AuthResponse;
import ru.oleynikova.finance.dto.LoginRequest;
import ru.oleynikova.finance.dto.RegisterRequest;
import ru.oleynikova.finance.dto.UserDto;
import ru.oleynikova.finance.security.AuthUser;
import ru.oleynikova.finance.service.AuthService;

// Отдельного logout нет, фронтенд просто удаляет токен
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Регистрация, вход, текущий пользователь")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация клиента (возвращает JWT)")
    public AuthResponse register(@RequestBody(required = false) RegisterRequest request) {
        return service.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Вход по email и паролю (возвращает JWT)")
    public AuthResponse login(@RequestBody(required = false) LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Текущий пользователь по токену")
    public UserDto me(@AuthenticationPrincipal AuthUser me) {
        return service.me(me.id());
    }
}
