package ru.oleynikova.finance.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import ru.oleynikova.finance.dto.AdminUserDto;
import ru.oleynikova.finance.dto.AutopaymentRunResult;
import ru.oleynikova.finance.dto.OperationDto;
import ru.oleynikova.finance.service.AdminService;
import ru.oleynikova.finance.service.AutopaymentService;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Администрирование (роль ADMIN)")
public class AdminController {

    private final AdminService admin;
    private final AutopaymentService autopayments;

    public AdminController(AdminService admin, AutopaymentService autopayments) {
        this.admin = admin;
        this.autopayments = autopayments;
    }

    @GetMapping("/users")
    @Operation(summary = "Все пользователи со сводкой по счетам")
    public List<AdminUserDto> users() {
        return admin.listUsers();
    }

    @PostMapping("/users/{id}/block")
    @Operation(summary = "Заблокировать пользователя")
    public AdminUserDto block(@PathVariable("id") String id) {
        return admin.setBlocked(id, true);
    }

    @PostMapping("/users/{id}/unblock")
    @Operation(summary = "Разблокировать пользователя")
    public AdminUserDto unblock(@PathVariable("id") String id) {
        return admin.setBlocked(id, false);
    }

    @GetMapping("/operations")
    @Operation(summary = "Все операции всех пользователей (новые сверху)")
    public List<OperationDto> operations(@RequestParam(name = "limit", required = false) String limit) {
        return admin.listOperations(limit);
    }

    @PostMapping("/autopayments/run")
    @Operation(summary = "Запустить автоплатежи, у которых наступила дата списания")
    public AutopaymentRunResult runAutopayments() {
        return autopayments.runDue();
    }
}
