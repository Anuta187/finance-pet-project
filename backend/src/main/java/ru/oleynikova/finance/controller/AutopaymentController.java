package ru.oleynikova.finance.controller;

import java.util.List;

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

import ru.oleynikova.finance.dto.AutopaymentDto;
import ru.oleynikova.finance.dto.CreateAutopaymentRequest;
import ru.oleynikova.finance.security.AuthUser;
import ru.oleynikova.finance.service.AutopaymentService;

@RestController
@RequestMapping("/api/autopayments")
@Tag(name = "Autopayments", description = "Регулярные платежи по расписанию")
public class AutopaymentController {

    private final AutopaymentService service;

    public AutopaymentController(AutopaymentService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Мои автоплатежи")
    public List<AutopaymentDto> list(@AuthenticationPrincipal AuthUser me) {
        return service.list(me.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать автоплатёж")
    public AutopaymentDto create(@AuthenticationPrincipal AuthUser me,
                                 @RequestBody(required = false) CreateAutopaymentRequest request) {
        return service.create(me.id(), request);
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "Отключить автоплатёж")
    public AutopaymentDto disable(@AuthenticationPrincipal AuthUser me, @PathVariable("id") String id) {
        return service.disable(me.id(), id);
    }
}
