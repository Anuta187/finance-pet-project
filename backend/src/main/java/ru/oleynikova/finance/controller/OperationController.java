package ru.oleynikova.finance.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.oleynikova.finance.dto.OperationDto;
import ru.oleynikova.finance.security.AuthUser;
import ru.oleynikova.finance.service.FinanceService;

@RestController
@RequestMapping("/api/operations")
@Tag(name = "Operations", description = "История операций")
public class OperationController {

    private final FinanceService service;

    public OperationController(FinanceService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "История моих операций (новые сверху)")
    public List<OperationDto> list(@AuthenticationPrincipal AuthUser me,
                                   @RequestParam(name = "accountId", required = false) String accountId,
                                   @RequestParam(name = "limit", required = false) String limit) {
        return service.listOperations(me.id(), accountId, limit);
    }
}
