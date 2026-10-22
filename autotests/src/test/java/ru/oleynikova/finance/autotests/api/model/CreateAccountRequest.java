package ru.oleynikova.finance.autotests.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;

// initialBalance Object, чтобы в негативных тестах отправлять что угодно
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CreateAccountRequest(String name, Object initialBalance) {
}
