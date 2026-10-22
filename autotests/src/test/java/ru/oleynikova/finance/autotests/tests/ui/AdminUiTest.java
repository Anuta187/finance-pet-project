package ru.oleynikova.finance.autotests.tests.ui;

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.exist;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.webdriver;
import static com.codeborne.selenide.WebDriverConditions.urlContaining;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.oleynikova.finance.autotests.data.AccountBuilder.anAccount;
import static ru.oleynikova.finance.autotests.ui.TestIds.testId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.codeborne.selenide.Selenide;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import ru.oleynikova.finance.autotests.api.client.AdminClient;
import ru.oleynikova.finance.autotests.api.client.AuthClient;
import ru.oleynikova.finance.autotests.data.Session;
import ru.oleynikova.finance.autotests.data.Users;
import ru.oleynikova.finance.autotests.tests.BaseUiTest;
import ru.oleynikova.finance.autotests.ui.pages.AdminPage;
import ru.oleynikova.finance.autotests.ui.pages.DashboardPage;
import ru.oleynikova.finance.autotests.ui.pages.LoginPage;

@Tag("ui")
@Epic("Личные финансы")
@Feature("UI: роли и админ-панель")
@DisplayName("UI: роли и админ-панель")
class AdminUiTest extends BaseUiTest {

    private final AdminPage admin = new AdminPage();
    private final LoginPage login = new LoginPage();

    @Test
    @Tag("smoke")
    @Story("US-14 Админ-панель")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E-14 Администратор после входа попадает в админ-панель: пользователи и журнал операций")
    void adminLandsOnAdminPanel() {
        logoutBrowser();
        login.open().login(Users.ADMIN_EMAIL, Users.ADMIN_PASSWORD);

        webdriver().shouldHave(urlContaining("/admin"));
        admin.waitLoaded().userRow(demo.userId()).shouldHave(text(Users.DEMO_EMAIL), text("Клиент"));
        admin.nav("transfers").shouldNot(exist);

        admin.openOperations().operationsTable().shouldBe(visible).shouldHave(text(Users.DEMO_EMAIL));
    }

    @Test
    @Story("US-13 Блокировка пользователя")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("E2E-15 Блокировка клиента в админке: статус меняется, клиент не может войти")
    void blockClientFromAdminPanel() {
        Session client = Users.newClient();
        anAccount(client).withBalance(100).create();
        loginAs(Users.admin());
        Selenide.open("/admin");

        admin.waitLoaded().toggleBlock(client.userId());

        admin.userRow(client.userId()).$(testId("admin-user-status")).shouldHave(exactText("Заблокирован"));
        admin.lastToast().shouldHave(text("заблокирован"));
        assertThat(new AuthClient().loginRaw(client.email(), Users.DEFAULT_PASSWORD).statusCode()).isEqualTo(403);

        admin.toggleBlock(client.userId());
        admin.userRow(client.userId()).$(testId("admin-user-status")).shouldHave(exactText("Активен"));
    }

    @Test
    @Story("US-13 Блокировка пользователя")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E-16 Заблокированного во время сессии клиента выкидывает на вход с причиной")
    void blockedClientIsLoggedOut() {
        Session client = Users.newClient();
        loginAs(client);
        new DashboardPage().open();

        new AdminClient(Users.admin()).block(client.userId());
        Selenide.refresh();

        login.notice().shouldHave(text("заблокирован"));
        assertThat(Selenide.localStorage().getItem(TOKEN_KEY)).isNull();
    }

    @Test
    @Story("US-11 Роли")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("E2E-17 Клиент, открывший /admin, видит 403 «Недостаточно прав»")
    void clientSeesForbidden() {
        loginAs(demo);
        Selenide.open("/admin");

        $(testId("forbidden")).shouldHave(text("Недостаточно прав"));
        $(testId("admin-users-table")).shouldNot(exist);
    }
}
