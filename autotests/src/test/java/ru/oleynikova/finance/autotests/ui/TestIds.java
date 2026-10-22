package ru.oleynikova.finance.autotests.ui;

import org.openqa.selenium.By;

public final class TestIds {

    private TestIds() {
    }

    public static By testId(String id) {
        return By.cssSelector("[data-testid='" + id + "']");
    }
}
