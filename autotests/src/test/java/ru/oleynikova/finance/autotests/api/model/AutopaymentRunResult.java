package ru.oleynikova.finance.autotests.api.model;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AutopaymentRunResult(String date, int processed, int succeeded, int failed, List<Item> results) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(Long autopaymentId, String status, String errorCode, Long operationId) {
    }

    // в запуске могут быть и чужие автоплатежи
    public Optional<Item> itemFor(long autopaymentId) {
        return results.stream().filter(i -> i.autopaymentId() == autopaymentId).findFirst();
    }
}
