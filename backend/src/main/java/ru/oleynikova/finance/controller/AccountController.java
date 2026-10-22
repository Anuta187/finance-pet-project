package ru.oleynikova.finance.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.oleynikova.finance.dto.AccountDetailsDto;
import ru.oleynikova.finance.dto.AccountDto;
import ru.oleynikova.finance.dto.CreateAccountRequest;
import ru.oleynikova.finance.security.AuthUser;
import ru.oleynikova.finance.service.FinanceService;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "Счета текущего пользователя")
public class AccountController {

    private final FinanceService service;

    public AccountController(FinanceService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Мои счета")
    public List<AccountDto> list(@AuthenticationPrincipal AuthUser me) {
        return service.listAccounts(me.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Открыть новый счёт")
    public AccountDto create(@AuthenticationPrincipal AuthUser me,
                             @Valid @RequestBody(required = false) CreateAccountRequest request) {
        return service.createAccount(me.id(), request == null ? new CreateAccountRequest(null, null) : request);
    }

    // id строкой, чтобы на "abc" вернуть свой VALIDATION_ERROR
    @GetMapping("/{id}")
    @Operation(summary = "Счёт по id (с информацией о дневном лимите)")
    public AccountDetailsDto get(@AuthenticationPrincipal AuthUser me, @PathVariable("id") String id) {
        return service.getAccount(me.id(), id);
    }
}
