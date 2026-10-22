package ru.oleynikova.finance.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.oleynikova.finance.dto.TransferRequest;
import ru.oleynikova.finance.dto.TransferResult;
import ru.oleynikova.finance.security.AuthUser;
import ru.oleynikova.finance.service.FinanceService;

@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Transfers", description = "Переводы")
public class TransferController {

    private final FinanceService service;

    public TransferController(FinanceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Перевод между своими счетами", description = "Выполняется атомарно в одной транзакции БД.")
    public TransferResult transfer(@AuthenticationPrincipal AuthUser me,
                                   @Valid @RequestBody(required = false) TransferRequest request) {
        return service.transfer(me.id(), request);
    }
}
