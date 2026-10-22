package ru.oleynikova.finance.autotests.tests.ui;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;
import static com.codeborne.selenide.Selenide.webdriver;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.codeborne.selenide.Selenide;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import ru.oleynikova.finance.autotests.data.TestData;
import ru.oleynikova.finance.autotests.data.Users;
import ru.oleynikova.finance.autotests.tests.BaseUiTest;
import ru.oleynikova.finance.autotests.ui.pages.DashboardPage;
import ru.oleynikova.finance.autotests.ui.pages.HistoryPage;
import ru.oleynikova.finance.autotests.ui.pages.LoginPage;

@Tag("ui")
@Epic("Личные финансы")
@Feature("UI: вход, регистрация, выход")
@DisplayName("UI: вход, регистрация, выход")
class AuthUiTest extends BaseUiTest {

    private final LoginPage login = new LoginPage();
    private final DashboardPage dashboard = new DashboardPage();

    @BeforeEach
    void anonymous() {
        logoutBrowser();
    }

    @Test
    @Tag("smoke")
    @Story("US-09 Вход")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("E2E-01 Вход демо-клиента через форму: дашборд с приветствием, счетами и итогом")
    void loginDemo() {
        login.open().login(Users.DEMO_EMAIL, Users.DEMO_PASSWORD);

        dashboard.greeting().shouldHave(exactText("Здравствуйте, Иван!"));
        dashboard.userName().shouldHave(exactText("Иван Петров"));
        dashboard.waitAccounts().accountRowByName("Основной").shouldBe(visible);
        dashboard.total().shouldHave(exactText("197 540,50 ₽"));
        dashboard.spendingChart().shouldBe(visible);
        dashboard.nav("admin").shouldNot(com.codeborne.selenide.Condition.exist);
    }

    @Test
    @Story("US-09 Вход")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E-02 Неверный пароль: сообщение INVALID_CREDENTIALS, остаёмся на странице входа")
    void wrongPassword() {
        login.open().login(Users.DEMO_EMAIL, "wrong-pass1");

        login.error().shouldHave(attribute("data-error-code", "INVALID_CREDENTIALS"));
        login.error().shouldHave(text("Неверный email или пароль"));
        webdriver().shouldHave(urlContaining("/login"));
    }

    @Test
    @Story("US-10 Защита API")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E-03 Без входа открывается /login, после входа возврат на исходную страницу")
    void guardRedirectsBack() {
        Selenide.open("/history");
        login.form().shouldBe(visible);

        login.login(Users.DEMO_EMAIL, Users.DEMO_PASSWORD);

        webdriver().shouldHave(urlContaining("/history"));
        new HistoryPage().rows().first().shouldBe(visible);
    }

    @Test
    @Story("US-08 Регистрация")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E-04 Регистрация нового клиента")
    void registerNewClient() {
        var register = login.open().goToRegister();
        String email = TestData.uniqueEmail();

        register.register("Мария Смирнова", email, "Secret123", "Secret124");
        register.error().shouldHave(exactText("Пароли не совпадают"));

        register.register("Мария Смирнова", email, "Secret123", "Secret123");

        dashboard.greeting().shouldHave(exactText("Здравствуйте, Мария!"));
        dashboard.accountsEmpty().shouldBe(visible);
        dashboard.total().shouldHave(exactText("0,00 ₽"));
        dashboard.lastToast().shouldHave(text("Добро пожаловать"));
    }

    @Test
    @Story("US-09 Вход")
    @DisplayName("E2E-05 Выход: токен удаляется, защищённые страницы снова недоступны")
    void logout() {
        loginAs(demo);
        dashboard.open();

        dashboard.logout().form().shouldBe(visible);

        assertThat(Selenide.localStorage().getItem(TOKEN_KEY)).isNull();
        Selenide.open("/");
        login.form().shouldBe(visible);
    }
}
