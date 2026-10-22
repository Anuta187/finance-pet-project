package ru.oleynikova.finance.autotests.tests.ui;

import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.data.TestData;
import ru.oleynikova.finance.autotests.tests.BaseUiTest;
import ru.oleynikova.finance.autotests.ui.pages.DashboardPage;
import ru.oleynikova.finance.autotests.ui.pages.HistoryPage;
import ru.oleynikova.finance.autotests.ui.pages.TransfersPage;

@Tag("ui")
@Epic("Личные финансы")
@Feature("UI: счета, переводы, история")
@DisplayName("UI: счета, переводы, история")
class BankingUiTest extends BaseUiTest {

    private final DashboardPage dashboard = new DashboardPage();
    private final TransfersPage transfersPage = new TransfersPage();
    private final HistoryPage history = new HistoryPage();

    @BeforeEach
    void login() {
        loginAs(demo);
    }

    @Test
    @Story("US-02 Открытие счёта")
    @DisplayName("E2E-06 Открытие счёта на главной")
    void createAccount() {
        String name = TestData.uniqueName("Отпуск");

        dashboard.open().waitAccounts().createAccount(name, "1500,5");

        dashboard.lastToast().shouldHave(text(name));
        dashboard.accountRowByName(name).$(testId("account-balance")).shouldHave(exactText("1 500,50 ₽"));

        dashboard.open().submitEmptyAccountForm();
        dashboard.createAccountError().shouldHave(exactText("Введите название счёта"));
    }

    @Test
    @Tag("smoke")
    @Story("US-03 Перевод между счетами")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("E2E-07 Успешный перевод: тост, балансы и история обновляются")
    void successfulTransfer() {
        Account from = anAccountOfDemo().withNamePrefix("Откуда").withBalance(1000).create();
        Account to = anAccountOfDemo().withNamePrefix("Куда").withBalance(0).create();

        transfersPage.open().transfer(from.id(), to.id(), "250", "E2E перевод");

        transfersPage.lastToast().shouldHave(text("Переведено 250,00 ₽"));
        transfersPage.balanceOf(from.id()).shouldHave(exactText("750,00 ₽"));
        transfersPage.balanceOf(to.id()).shouldHave(exactText("250,00 ₽"));

        history.open();
        var row = history.rowWithText("E2E перевод");
        row.$(testId("history-type")).shouldHave(exactText("Перевод"));
        row.$(testId("history-from")).shouldHave(exactText(from.name()));
        row.$(testId("history-to")).shouldHave(exactText(to.name()));
        row.$(testId("history-amount")).shouldHave(exactText("250,00 ₽"));
    }

    @Test
    @Story("US-05 Проверка остатка")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E-08 Ошибки перевода: недостаточно средств и тот же счёт; балансы не меняются")
    void transferErrors() {
        Account from = anAccountOfDemo().withBalance(100).create();
        Account to = anAccountOfDemo().withBalance(0).create();

        transfersPage.open().transfer(from.id(), to.id(), "100.01", null);
        transfersPage.error().shouldHave(attribute("data-error-code", "INSUFFICIENT_FUNDS"));
        transfersPage.error().shouldHave(exactText("Недостаточно средств на счёте списания"));

        transfersPage.transfer(from.id(), from.id(), "10", null);
        transfersPage.error().shouldHave(attribute("data-error-code", "SAME_ACCOUNT"));

        transfersPage.balanceOf(from.id()).shouldHave(exactText("100,00 ₽"));
        transfersPage.balanceOf(to.id()).shouldHave(exactText("0,00 ₽"));
    }

    @Test
    @Story("US-07 История операций")
    @DisplayName("E2E-09 Фильтр истории по счёту")
    void historyFilter() {
        Account acc = anAccountOfDemo().withNamePrefix("Фильтр").withBalance(42).create();

        history.open().filterBy(acc.id());

        history.rows().shouldHave(size(1));
        history.rows().first().$(testId("history-type")).shouldHave(exactText("Пополнение"));
        history.rows().first().$(testId("history-amount")).shouldHave(exactText("42,00 ₽"));
    }
}
