package ru.oleynikova.finance.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.oleynikova.finance.dto.CategoryDto;
import ru.oleynikova.finance.dto.PaymentRequest;
import ru.oleynikova.finance.dto.PaymentResult;
import ru.oleynikova.finance.dto.SpendingDto;
import ru.oleynikova.finance.security.AuthUser;
import ru.oleynikova.finance.service.PaymentService;
import ru.oleynikova.finance.service.StatsService;

@RestController
@Tag(name = "Payments", description = "Оплата услуг по категориям и аналитика расходов")
public class PaymentController {

    private final PaymentService payments;
    private final StatsService stats;

    public PaymentController(PaymentService payments, StatsService stats) {
        this.payments = payments;
        this.stats = stats;
    }

    @GetMapping("/api/categories")
    @Operation(summary = "Справочник категорий расходов")
    public List<CategoryDto> categories() {
        return payments.categories();
    }

    @PostMapping("/api/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Оплата услуг по категории со счёта или по карте")
    public PaymentResult pay(@AuthenticationPrincipal AuthUser me, @RequestBody(required = false) PaymentRequest request) {
        return payments.pay(me.id(), request);
    }

    @GetMapping("/api/stats/spending")
    @Operation(summary = "Расходы по категориям за месяц (по умолчанию — текущий)")
    public SpendingDto spending(@AuthenticationPrincipal AuthUser me,
                                @RequestParam(name = "month", required = false) String month) {
        return stats.spending(me.id(), month);
    }
}
