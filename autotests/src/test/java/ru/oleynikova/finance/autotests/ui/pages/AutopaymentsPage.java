package ru.oleynikova.finance.autotests.ui.pages;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import io.qameta.allure.Step;

public class AutopaymentsPage extends AppShell {

    @Step("Открыть автоплатежи")
    public AutopaymentsPage open() {
        Selenide.open("/autopayments");
        $(testId("autopayment-form")).should(exist);
        return this;
    }

    public ElementsCollection rows() {
        return $$(testId("autopayment-row"));
    }

    public SelenideElement rowByName(String name) {
        return rows().findBy(text(name));
    }

    public SelenideElement error() {
        return $(testId("autopayment-error"));
    }

    @Step("Создать автоплатёж «{name}»: {period}, {amount} с {source}")
    public AutopaymentsPage create(String name, String categoryId, String source, String amount, String period) {
        $(testId("autopayment-name")).setValue(name);
        SelenideElement category = $(testId("autopayment-category"));
        category.$("option[value='" + categoryId + "']").should(exist);
        category.selectOptionByValue(categoryId);
        SelenideElement src = $(testId("autopayment-source"));
        src.$("option[value='" + source + "']").should(exist);
        src.selectOptionByValue(source);
        $(testId("autopayment-amount")).setValue(amount);
        $(testId("autopayment-period")).selectOptionByValue(period);
        $(testId("autopayment-submit")).click();
        return this;
    }

    @Step("Отключить автоплатёж «{name}»")
    public AutopaymentsPage disable(String name) {
        rowByName(name).$(testId("autopayment-disable")).click();
        return this;
    }
}
