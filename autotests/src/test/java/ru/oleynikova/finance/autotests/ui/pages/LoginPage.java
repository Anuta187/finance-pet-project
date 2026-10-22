package ru.oleynikova.finance.autotests.ui.pages;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;

import io.qameta.allure.Step;

public class LoginPage {

    public SelenideElement form() {
        return $(testId("login-form"));
    }

    public SelenideElement error() {
        return $(testId("login-error"));
    }

    public SelenideElement notice() {
        return $(testId("login-notice"));
    }

    @Step("Открыть страницу входа")
    public LoginPage open() {
        Selenide.open("/login");
        form().shouldBe(visible);
        return this;
    }

    @Step("Войти как {email}")
    public LoginPage login(String email, String password) {
        $(testId("login-email")).setValue(email);
        $(testId("login-password")).setValue(password);
        $(testId("login-submit")).click();
        return this;
    }

    @Step("Перейти к регистрации")
    public RegisterPage goToRegister() {
        $(testId("to-register")).click();
        return new RegisterPage();
    }
}
