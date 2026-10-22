package ru.oleynikova.finance.entity;

import java.time.LocalDate;

public enum AutopaymentPeriod {
    DAILY,
    WEEKLY,
    MONTHLY;

    // Считаем от даты старта, чтобы платёж 31-го числа не съезжал после февраля
    public LocalDate nth(LocalDate start, int n) {
        return switch (this) {
            case DAILY -> start.plusDays(n);
            case WEEKLY -> start.plusWeeks(n);
            case MONTHLY -> start.plusMonths(n);
        };
    }
}
