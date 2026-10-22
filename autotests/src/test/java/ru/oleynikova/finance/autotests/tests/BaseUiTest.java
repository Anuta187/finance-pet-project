package ru.oleynikova.finance.autotests.tests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.chrome.ChromeOptions;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.logevents.SelenideLogger;

import io.qameta.allure.Allure;
import io.qameta.allure.selenide.AllureSelenide;

import ru.oleynikova.finance.autotests.config.TestConfig;
import ru.oleynikova.finance.autotests.data.Session;

public abstract class BaseUiTest extends BaseTest {

    public static final String TOKEN_KEY = "finance.token";

    @BeforeAll
    static void setUpBrowser() {
        Configuration.baseUrl = TestConfig.baseUrl();
        Configuration.browser = TestConfig.browser();
        Configuration.headless = TestConfig.headless();
        Configuration.browserSize = TestConfig.browserSize();
        Configuration.timeout = TestConfig.timeoutMs();
        Configuration.pageLoadTimeout = 30_000;
        Configuration.browserCapabilities = new ChromeOptions()
                .addArguments("--no-sandbox", "--disable-dev-shm-usage", "--lang=ru-RU");

        SelenideLogger.addListener("AllureSelenide", new AllureSelenide().screenshots(true).savePageSource(false));
    }

    @AfterAll
    static void tearDownBrowser() {
        Selenide.closeWebDriver();
        SelenideLogger.removeListener("AllureSelenide");
    }

    // Вход без формы: кладём токен из API в localStorage
    protected void loginAs(Session session) {
        Allure.step("Войти в UI как " + session.email() + " (токен в localStorage)", () -> {
            Selenide.open("/login");
            Selenide.localStorage().setItem(TOKEN_KEY, session.token());
        });
    }

    protected void logoutBrowser() {
        Selenide.open("/login");
        Selenide.clearBrowserLocalStorage();
        Selenide.sessionStorage().clear();
    }
}
