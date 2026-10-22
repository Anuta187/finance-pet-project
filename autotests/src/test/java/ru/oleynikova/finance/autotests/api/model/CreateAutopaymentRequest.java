package ru.oleynikova.finance.autotests.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateAutopaymentRequest(String name, Object categoryId, Object accountId, Object cardId, Object amount,
                                       String period, String startDate) {

    public static CreateAutopaymentRequest monthlyFromAccount(String name, long categoryId, long accountId,
                                                              Object amount, String startDate) {
        return new CreateAutopaymentRequest(name, categoryId, accountId, null, amount, "MONTHLY", startDate);
    }
}
