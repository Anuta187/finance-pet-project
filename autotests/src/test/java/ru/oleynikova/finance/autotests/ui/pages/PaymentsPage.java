package ru.oleynikova.finance.autotests.ui.pages;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import io.qameta.allure.Step;

public class PaymentsPage extends AppShell {

    @Step("Открыть оплату услуг")
    public PaymentsPage open() {
        Selenide.open("/payments");
        category("MOBILE").shouldBe(visible);
        return this;
    }

    public SelenideElement category(String code) {
        return $(testId("category-" + code));
    }

    public SelenideElement error() {
        return $(testId("payment-error"));
    }

    // value: "account:ID" или "card:ID"
    public SelenideElement sourceOption(String value) {
        return $(testId("payment-source")).$("option[value='" + value + "']");
    }

    @Step("Оплатить {code}: {amount} с источника {source}")
    public PaymentsPage pay(String code, String source, String amount, String comment) {
        category(code).click();
        sourceOption(source).should(exist);
        $(testId("payment-source")).selectOptionByValue(source);
        $(testId("payment-amount")).setValue(amount);
        $(testId("payment-comment")).setValue(comment == null ? "" : comment);
        $(testId("payment-submit")).click();
        return this;
    }
}
