package ru.oleynikova.finance.autotests.tests;

import static ru.oleynikova.finance.autotests.data.AccountBuilder.anAccount;

import org.junit.jupiter.api.BeforeAll;

import io.qameta.allure.Allure;

import ru.oleynikova.finance.autotests.api.client.AccountsClient;
import ru.oleynikova.finance.autotests.api.client.OperationsClient;
import ru.oleynikova.finance.autotests.api.client.ServiceClient;
import ru.oleynikova.finance.autotests.api.client.TransfersClient;
import ru.oleynikova.finance.autotests.config.TestConfig;
import ru.oleynikova.finance.autotests.data.AccountBuilder;
import ru.oleynikova.finance.autotests.data.Session;
import ru.oleynikova.finance.autotests.data.Users;

/** Перед каждым тест-классом сбрасывает базу и входит демо-клиентом. */
public abstract class BaseTest {

    protected static Session demo;

    protected final ServiceClient service = new ServiceClient();
    protected final AccountsClient accounts = new AccountsClient(demo);
    protected final TransfersClient transfers = new TransfersClient(demo);
    protected final OperationsClient operations = new OperationsClient(demo);

    @BeforeAll
    static void resetDatabaseAndLogin() {
        if (TestConfig.resetDb()) {
            int status = new ServiceClient().reset().statusCode();
            if (status != 204) {
                Allure.step("Сброс БД недоступен (HTTP " + status + "): бэкенд запущен не в профиле test, "
                        + "тесты работают на своих уникальных данных");
            }
        }
        demo = Users.demo();
    }

    protected static AccountBuilder anAccountOfDemo() {
        return anAccount(demo);
    }
}
