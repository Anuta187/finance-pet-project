package ru.oleynikova.finance.dto;

import java.time.LocalDate;
import java.util.List;

import ru.oleynikova.finance.entity.AutopaymentStatus;

public record AutopaymentRunResult(LocalDate date, int processed, int succeeded, int failed, List<Item> results) {

    public record Item(Long autopaymentId, AutopaymentStatus status, String errorCode, Long operationId) {
    }
}
