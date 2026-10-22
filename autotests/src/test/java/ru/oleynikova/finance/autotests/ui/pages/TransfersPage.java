package ru.oleynikova.finance.autotests.ui.pages;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import io.qameta.allure.Step;

public class TransfersPage extends AppShell {

    @Step("Открыть страницу переводов")
    public TransfersPage open() {
        Selenide.open("/transfers");
        $(testId("transfer-form")).shouldBe(visible);
        return this;
    }

    public SelenideElement error() {
        return $(testId("transfer-error"));
    }

    public SelenideElement balanceOf(long accountId) {
        return $("[data-testid='account-row'][data-account-id='" + accountId + "']").$(testId("account-balance"));
    }

    @Step("Перевести {amount} со счёта {fromId} на счёт {toId}, комментарий «{comment}»")
    public TransfersPage transfer(long fromId, long toId, String amount, String comment) {
        select("transfer-from", fromId);
        select("transfer-to", toId);
        $(testId("transfer-amount")).setValue(amount);
        $(testId("transfer-comment")).setValue(comment == null ? "" : comment);
        $(testId("transfer-submit")).click();
        return this;
    }

    private void select(String selectTestId, long accountId) {
        SelenideElement select = $(testId(selectTestId));
        // опции подгружаются асинхронно
        select.$("option[value='" + accountId + "']").should(exist);
        select.selectOptionByValue(String.valueOf(accountId));
    }
}
