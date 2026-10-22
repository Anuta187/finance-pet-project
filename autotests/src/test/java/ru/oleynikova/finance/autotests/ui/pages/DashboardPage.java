package ru.oleynikova.finance.autotests.ui.pages;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import io.qameta.allure.Step;

public class DashboardPage extends AppShell {

    @Step("Открыть главную страницу")
    public DashboardPage open() {
        Selenide.open("/");
        greeting().shouldBe(visible);
        return this;
    }

    public SelenideElement greeting() {
        return $(testId("greeting"));
    }

    public SelenideElement total() {
        return $(testId("accounts-total"));
    }

    public SelenideElement accountsEmpty() {
        return $(testId("accounts-empty"));
    }

    public ElementsCollection accountRows() {
        return $$(testId("account-row"));
    }

    public DashboardPage waitAccounts() {
        accountRows().shouldHave(sizeGreaterThan(0));
        return this;
    }

    public SelenideElement accountRow(long accountId) {
        return $("[data-testid='account-row'][data-account-id='" + accountId + "']");
    }

    public SelenideElement accountRowByName(String name) {
        return accountRows().findBy(text(name));
    }

    public SelenideElement balanceOf(long accountId) {
        return accountRow(accountId).$(testId("account-balance"));
    }

    public SelenideElement spendingChart() {
        return $(testId("spending-chart"));
    }

    public SelenideElement spendingItem(String categoryCode) {
        return $("[data-testid='spending-item'][data-category='" + categoryCode + "']");
    }

    public SelenideElement spendingTotal() {
        return $(testId("spending-total"));
    }

    public SelenideElement createAccountError() {
        return $(testId("create-account-error"));
    }

    @Step("Открыть счёт «{name}» с остатком {initialBalance}")
    public DashboardPage createAccount(String name, String initialBalance) {
        $(testId("toggle-create-account")).click();
        $(testId("account-name-input")).setValue(name);
        $(testId("account-initial-input")).setValue(initialBalance);
        $(testId("create-account-submit")).click();
        return this;
    }

    @Step("Нажать «Открыть счёт» с пустой формой")
    public DashboardPage submitEmptyAccountForm() {
        $(testId("toggle-create-account")).click();
        $(testId("create-account-submit")).click();
        return this;
    }
}
