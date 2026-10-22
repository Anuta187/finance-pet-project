package ru.oleynikova.finance.autotests.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.oleynikova.finance.autotests.assertions.ApiAssertions.assertError;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.api.model.AccountDetails;
import ru.oleynikova.finance.autotests.tests.BaseTest;

@Tag("api")
@Epic("Личные финансы")
@Feature("API: дневной лимит 100 000 ₽")
@Story("US-06 Дневной лимит")
@DisplayName("API: дневной лимит 100 000 ₽")
class DailyLimitApiTest extends BaseTest {

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-21 Ровно 100 000 проходит, следующий 0.01 нет")
    void exactLimitThenOneKopeck() {
        Account from = anAccountOfDemo().withBalance(200000).create();
        Account to = anAccountOfDemo().create();

        transfers.transfer(from.id(), to.id(), 60000);
        transfers.transfer(from.id(), to.id(), 40000);
        AccountDetails details = accounts.get(from.id());
        assertThat(details.dailyTransferred()).isEqualByComparingTo("100000");
        assertThat(details.dailyLimitRemaining()).isEqualByComparingTo("0");

        var error = assertError(transfers.transferRaw(from.id(), to.id(), 0.01), 422, "DAILY_LIMIT_EXCEEDED");
        assertThat(new BigDecimal(error.details().get("dailyLimitRemaining").toString())).isEqualByComparingTo("0");
        assertThat(accounts.get(from.id()).balance()).isEqualByComparingTo("100000");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-22 Один перевод на 100 000.01: DAILY_LIMIT_EXCEEDED")
    void singleTransferOverLimit() {
        Account from = anAccountOfDemo().withBalance(200000).create();
        Account to = anAccountOfDemo().create();

        assertError(transfers.transferRaw(from.id(), to.id(), 100000.01), 422, "DAILY_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("AT-23 Лимит считается по счёту списания: другой счёт не затронут")
    void limitIsPerSourceAccount() {
        Account a = anAccountOfDemo().withBalance(150000).create();
        Account b = anAccountOfDemo().withBalance(150000).create();

        transfers.transfer(a.id(), b.id(), 100000);
        // b получил деньги, но его собственный лимит списания не израсходован
        transfers.transfer(b.id(), a.id(), 100000);
    }
}
