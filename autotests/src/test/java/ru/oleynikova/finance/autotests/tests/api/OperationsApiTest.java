package ru.oleynikova.finance.autotests.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.oleynikova.finance.autotests.assertions.ApiAssertions.assertError;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.api.model.Operation;
import ru.oleynikova.finance.autotests.tests.BaseTest;

@Tag("api")
@Epic("Личные финансы")
@Feature("API: история операций")
@Story("US-07 История операций")
@DisplayName("API: история операций")
class OperationsApiTest extends BaseTest {

    @Test
    @DisplayName("AT-24 Фильтр по счёту и сортировка «новые сверху»")
    void filterAndSort() {
        Account a = anAccountOfDemo().withBalance(1000).create();
        Account b = anAccountOfDemo().create();
        Account c = anAccountOfDemo().create();
        transfers.transfer(a.id(), b.id(), 100);
        transfers.transfer(b.id(), c.id(), 30);

        List<Operation> ops = operations.byAccount(b.id());

        assertThat(ops).hasSize(2);
        Operation newest = ops.get(0);
        assertThat(newest.fromAccountId()).isEqualTo(b.id());
        assertThat(newest.toAccountId()).isEqualTo(c.id());
        assertThat(newest.amount()).isEqualByComparingTo("30");
        assertThat(newest.fromAccountName()).isEqualTo(b.name());
        assertThat(newest.toAccountName()).isEqualTo(c.name());
        assertThat(ops.get(1).fromAccountId()).isEqualTo(a.id());
        assertThat(ops.get(1).toAccountId()).isEqualTo(b.id());
        assertThat(ops.get(1).amount()).isEqualByComparingTo("100");
        assertThat(newest.id()).isGreaterThan(ops.get(1).id());
    }

    @Test
    @DisplayName("AT-25 Общий список содержит последние операции; limit работает")
    void limitParameter() {
        Account a = anAccountOfDemo().withBalance(10).create();
        Account b = anAccountOfDemo().create();
        transfers.transfer(a.id(), b.id(), 1);

        assertThat(operations.list(Map.of("limit", 1))).hasSize(1);
    }

    @Test
    @DisplayName("AT-26 Невалидные параметры: 400 / 404")
    void invalidParameters() {
        assertError(operations.listRaw(Map.of("limit", 0)), 400, "VALIDATION_ERROR");
        assertError(operations.listRaw(Map.of("accountId", "abc")), 400, "VALIDATION_ERROR");
        assertError(operations.listRaw(Map.of("accountId", 999999)), 404, "ACCOUNT_NOT_FOUND");
    }
}
