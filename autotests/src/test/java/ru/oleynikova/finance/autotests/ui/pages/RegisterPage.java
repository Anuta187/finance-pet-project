package ru.oleynikova.finance.autotests.ui.pages;

import static com.codeborne.selenide.Selenide.$;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import com.codeborne.selenide.SelenideElement;

import io.qameta.allure.Step;

public class RegisterPage {

    public SelenideElement error() {
        return $(testId("register-error"));
    }

    @Step("Зарегистрироваться: {fullName}, {email}")
    public RegisterPage register(String fullName, String email, String password, String password2) {
        $(testId("register-name")).setValue(fullName);
        $(testId("register-email")).setValue(email);
        $(testId("register-password")).setValue(password);
        $(testId("register-password2")).setValue(password2);
        $(testId("register-submit")).click();
        return this;
    }
}
