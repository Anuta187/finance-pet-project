package ru.oleynikova.finance.autotests.ui.pages;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.SelenideElement;

import io.qameta.allure.Step;

/** Меню, шапка и уведомления, общие для всех страниц. */
public class AppShell {

    public SelenideElement nav(String id) {
        return $(testId("nav-" + id));
    }

    public SelenideElement userName() {
        return $(testId("user-name"));
    }

    public ElementsCollection toasts() {
        return $$(testId("toast"));
    }

    public SelenideElement lastToast() {
        return toasts().last();
    }

    @Step("Перейти в раздел «{id}» через меню")
    public AppShell go(String id) {
        nav(id).click();
        return this;
    }

    @Step("Выйти из аккаунта")
    public LoginPage logout() {
        $(testId("logout")).click();
        return new LoginPage();
    }
}
