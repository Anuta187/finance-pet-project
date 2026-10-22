package ru.oleynikova.finance.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Запуск автоплатежей раз в минуту. В профиле test выключен. */
@Component
@ConditionalOnProperty(name = "app.autopayments.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class AutopaymentScheduler {

    private final AutopaymentService service;

    public AutopaymentScheduler(AutopaymentService service) {
        this.service = service;
    }

    @Scheduled(cron = "${app.autopayments.cron:0 * * * * *}")
    public void run() {
        service.runDue();
    }
}
