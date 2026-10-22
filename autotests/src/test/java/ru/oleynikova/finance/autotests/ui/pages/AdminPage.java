package ru.oleynikova.finance.autotests.ui.pages;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import com.codeborne.selenide.SelenideElement;

import io.qameta.allure.Step;

public class AdminPage extends AppShell {

    public SelenideElement usersTable() {
        return $(testId("admin-users-table"));
    }

    public SelenideElement userRow(long userId) {
        return $("[data-testid='admin-user-row'][data-user-id='" + userId + "']");
    }

    public SelenideElement operationsTable() {
        return $(testId("admin-operations-table"));
    }

    public AdminPage waitLoaded() {
        usersTable().shouldBe(visible);
        return this;
    }

    @Step("Заблокировать/разблокировать пользователя {userId}")
    public AdminPage toggleBlock(long userId) {
        userRow(userId).$(testId("admin-block-toggle")).click();
        return this;
    }

    @Step("Открыть вкладку «Все операции»")
    public AdminPage openOperations() {
        $(testId("tab-operations")).click();
        return this;
    }

    @Step("Нажать «Выполнить автоплатежи»")
    public AdminPage runAutopayments() {
        $(testId("admin-run-autopayments")).click();
        return this;
    }
}
