package ru.oleynikova.finance.autotests.ui.pages;

import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import io.qameta.allure.Step;

public class CardsPage extends AppShell {

    @Step("Открыть карты")
    public CardsPage open() {
        Selenide.open("/cards");
        $(testId("issue-card-account")).should(exist);
        return this;
    }

    public ElementsCollection cards() {
        return $$(testId("card-item"));
    }

    public SelenideElement card(long cardId) {
        return $("[data-testid='card-item'][data-card-id='" + cardId + "']");
    }

    @Step("Выпустить карту к счёту {accountId}")
    public CardsPage issue(long accountId) {
        SelenideElement select = $(testId("issue-card-account"));
        select.$("option[value='" + accountId + "']").should(exist);
        select.selectOptionByValue(String.valueOf(accountId));
        $(testId("issue-card-submit")).click();
        return this;
    }

    @Step("Переключить блокировку карты {cardId}")
    public CardsPage toggle(long cardId) {
        card(cardId).$(testId("card-toggle")).click();
        return this;
    }
}
