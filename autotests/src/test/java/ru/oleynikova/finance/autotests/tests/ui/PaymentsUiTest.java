package ru.oleynikova.finance.autotests.tests.ui;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.disabled;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static ru.oleynikova.finance.autotests.data.AccountBuilder.anAccount;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import ru.oleynikova.finance.autotests.api.client.CardsClient;
import ru.oleynikova.finance.autotests.api.client.PaymentsClient;
import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.api.model.Card;
import ru.oleynikova.finance.autotests.data.Session;
import ru.oleynikova.finance.autotests.data.Users;
import ru.oleynikova.finance.autotests.tests.BaseUiTest;
import ru.oleynikova.finance.autotests.ui.pages.AutopaymentsPage;
import ru.oleynikova.finance.autotests.ui.pages.CardsPage;
import ru.oleynikova.finance.autotests.ui.pages.DashboardPage;
import ru.oleynikova.finance.autotests.ui.pages.PaymentsPage;

@Tag("ui")
@Epic("Личные финансы")
@Feature("UI: платежи, карты, автоплатежи")
@DisplayName("UI: платежи, карты, автоплатежи")
class PaymentsUiTest extends BaseUiTest {

    private final PaymentsPage paymentsPage = new PaymentsPage();
    private final CardsPage cardsPage = new CardsPage();
    private final AutopaymentsPage autopaymentsPage = new AutopaymentsPage();
    private final DashboardPage dashboard = new DashboardPage();

    @Test
    @Tag("smoke")
    @Story("US-16 Оплата услуг")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("E2E-10 Оплата услуги: тост, баланс уменьшился, категория появилась в диаграмме расходов")
    void payAndSeeSpending() {
        Session client = Users.newClient();
        Account account = anAccount(client).named("Зарплатный").withBalance(5000).create();
        loginAs(client);

        paymentsPage.open().pay("INTERNET", "account:" + account.id(), "890", "Договор 123");

        paymentsPage.lastToast().shouldHave(text("Оплачено 890,00 ₽: Интернет и ТВ"));
        dashboard.open();
        dashboard.balanceOf(account.id()).shouldHave(exactText("4 110,00 ₽"));
        dashboard.spendingItem("INTERNET").shouldBe(visible)
                .$(testId("spending-amount")).shouldHave(exactText("890,00 ₽"));
        dashboard.spendingTotal().shouldHave(exactText("890,00 ₽"));
    }

    @Test
    @Story("US-16 Оплата услуг")
    @DisplayName("E2E-11 Оплата больше остатка: ошибка INSUFFICIENT_FUNDS")
    void payInsufficientFunds() {
        Account account = anAccountOfDemo().withBalance(10).create();
        loginAs(demo);

        paymentsPage.open().pay("MOBILE", "account:" + account.id(), "10.01", null);

        paymentsPage.error().shouldHave(attribute("data-error-code", "INSUFFICIENT_FUNDS"));
    }

    @Test
    @Story("US-15 Карты")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E-12 Выпуск и блокировка карты: заблокированная карта недоступна как источник оплаты")
    void issueAndBlockCard() {
        Session client = Users.newClient();
        Account account = anAccount(client).withBalance(1000).create();
        loginAs(client);

        cardsPage.open().issue(account.id());
        cardsPage.lastToast().shouldHave(text("выпущена"));
        Card card = new CardsClient(client).list().get(0);
        var item = cardsPage.card(card.id());
        item.$(testId("card-number")).shouldHave(exactText("2200 •••• •••• " + card.last4()));
        item.$(testId("card-status")).shouldHave(exactText("Активна"));

        cardsPage.toggle(card.id());
        item.$(testId("card-status")).shouldHave(exactText("Заблокирована"));
        item.$(testId("card-toggle")).shouldHave(exactText("Разблокировать"));

        paymentsPage.open().sourceOption("card:" + card.id()).shouldBe(disabled).shouldHave(text("заблокирована"));
        paymentsPage.sourceOption("account:" + account.id()).shouldBe(enabled);
    }

    @Test
    @Story("US-18 Автоплатежи")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E-13 Создание и отключение автоплатежа")
    void createAndDisableAutopayment() {
        Session client = Users.newClient();
        Account account = anAccount(client).withBalance(1000).create();
        long utilities = new PaymentsClient(client).category("UTILITIES").id();
        loginAs(client);

        autopaymentsPage.open().create("Квартплата", String.valueOf(utilities), "account:" + account.id(), "4320,50", "MONTHLY");

        autopaymentsPage.lastToast().shouldHave(text("Автоплатёж «Квартплата» создан"));
        var row = autopaymentsPage.rowByName("Квартплата");
        row.shouldHave(text("ЖКХ"), text("4 320,50 ₽"), text("Ежемесячно"));
        row.$(testId("autopayment-status")).shouldHave(exactText("Активен"));

        autopaymentsPage.disable("Квартплата");
        row.$(testId("autopayment-status")).shouldHave(exactText("Отключён"));
    }
}
