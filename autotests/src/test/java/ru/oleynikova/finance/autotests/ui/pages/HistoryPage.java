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

public class HistoryPage extends AppShell {

    @Step("Открыть историю операций")
    public HistoryPage open() {
        Selenide.open("/history");
        $(testId("history-filter")).should(exist);
        return this;
    }

    public ElementsCollection rows() {
        return $$(testId("history-row"));
    }

    public SelenideElement rowWithText(String value) {
        return rows().findBy(text(value));
    }

    @Step("Отфильтровать историю по счёту {accountId}")
    public HistoryPage filterBy(long accountId) {
        SelenideElement select = $(testId("history-filter"));
        select.$("option[value='" + accountId + "']").should(exist);
        select.selectOptionByValue(String.valueOf(accountId));
        $("[data-loaded='true']").should(exist);
        return this;
    }
}
