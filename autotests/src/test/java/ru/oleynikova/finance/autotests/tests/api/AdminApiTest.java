package ru.oleynikova.finance.autotests.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.oleynikova.finance.autotests.assertions.ApiAssertions.assertError;
import static ru.oleynikova.finance.autotests.data.AccountBuilder.anAccount;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import ru.oleynikova.finance.autotests.api.client.AdminClient;
import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.api.model.AdminUser;
import ru.oleynikova.finance.autotests.api.model.Operation;
import ru.oleynikova.finance.autotests.data.Session;
import ru.oleynikova.finance.autotests.data.Users;
import ru.oleynikova.finance.autotests.tests.BaseTest;

@Tag("api")
@Epic("Личные финансы")
@Feature("API: администрирование")
@Story("US-14 Админ-панель")
@DisplayName("API: администрирование")
class AdminApiTest extends BaseTest {

    private Session adminSession;
    private AdminClient admin;

    @BeforeEach
    void loginAdmin() {
        adminSession = Users.admin();
        admin = new AdminClient(adminSession);
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-40 Список пользователей: админ и демо-клиент со счётчиком счетов и суммой остатков")
    void listUsers() {
        List<AdminUser> users = admin.users();

        assertThat(users).extracting(AdminUser::email).contains(Users.ADMIN_EMAIL, Users.DEMO_EMAIL);
        AdminUser demoUser = admin.user(demo.userId());
        assertThat(demoUser.role()).isEqualTo("CLIENT");
        assertThat(demoUser.blocked()).isFalse();
        assertThat(demoUser.accountsCount()).isEqualTo(accounts.list().size());
        assertThat(demoUser.totalBalance()).isEqualByComparingTo(
                accounts.list().stream().map(Account::balance).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        assertThat(admin.user(adminSession.userId()).role()).isEqualTo("ADMIN");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-41 Блокировка и разблокировка клиента меняют флаг blocked")
    void blockAndUnblock() {
        Session client = Users.newClient();

        assertThat(admin.block(client.userId()).blocked()).isTrue();
        assertThat(admin.user(client.userId()).blocked()).isTrue();
        // повторная блокировка идемпотентна
        assertThat(admin.block(client.userId()).blocked()).isTrue();

        assertThat(admin.unblock(client.userId()).blocked()).isFalse();
        assertThat(admin.user(client.userId()).blocked()).isFalse();
    }

    @Test
    @DisplayName("AT-42 Ошибки блокировки: администратор, несуществующий пользователь, некорректный id")
    void blockNegative() {
        assertError(admin.blockRaw(adminSession.userId()), 422, "CANNOT_BLOCK_ADMIN");
        assertError(admin.blockRaw(999999), 404, "USER_NOT_FOUND");
        assertError(admin.blockRaw("abc"), 400, "VALIDATION_ERROR");
        assertThat(admin.user(adminSession.userId()).blocked()).isFalse();
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-43 Журнал всех операций содержит операции разных клиентов с email владельца")
    void allOperations() {
        Session client = Users.newClient();
        Account account = anAccount(client).withBalance(777).create();

        List<Operation> ops = admin.operations();

        assertThat(ops).anySatisfy(op -> {
            assertThat(op.toAccountId()).isEqualTo(account.id());
            assertThat(op.ownerEmail()).isEqualTo(client.email());
            assertThat(op.amount()).isEqualByComparingTo("777");
        });
        assertThat(ops).anySatisfy(op -> assertThat(op.ownerEmail()).isEqualTo(Users.DEMO_EMAIL));
        // клиентский эндпоинт не раскрывает владельца
        assertThat(operations.list(java.util.Map.of())).allSatisfy(op -> assertThat(op.ownerEmail()).isNull());
    }
}
