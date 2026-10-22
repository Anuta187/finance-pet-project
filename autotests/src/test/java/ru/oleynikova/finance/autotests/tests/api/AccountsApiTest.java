package ru.oleynikova.finance.autotests.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.oleynikova.finance.autotests.assertions.ApiAssertions.assertError;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.api.model.AccountDetails;
import ru.oleynikova.finance.autotests.api.model.CreateAccountRequest;
import ru.oleynikova.finance.autotests.api.model.Operation;
import ru.oleynikova.finance.autotests.data.TestData;
import ru.oleynikova.finance.autotests.tests.BaseTest;

@Tag("api")
@Epic("Личные финансы")
@Feature("API: счета")
@DisplayName("API: счета")
class AccountsApiTest extends BaseTest {

    @Test
    @Tag("smoke")
    @Story("US-01 Просмотр счетов")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-01 GET /api/accounts возвращает список с демо-счетами")
    void listContainsDemoAccounts() {
        List<Account> list = accounts.list();

        assertThat(list).extracting(Account::name).contains("Основной", "Накопительный", "Наличные");
        assertThat(list).allSatisfy(a -> {
            assertThat(a.id()).isPositive();
            assertThat(a.name()).isNotBlank();
            assertThat(a.currency()).isEqualTo("RUB");
            assertThat(a.balance()).isNotNull();
        });
    }

    @Test
    @Story("US-02 Открытие счёта")
    @DisplayName("AT-02 Счёт без начального остатка создаётся с балансом 0")
    void createWithoutBalance() {
        String name = TestData.uniqueName("Отпуск");

        Account acc = accounts.create(new CreateAccountRequest(name, null));

        assertThat(acc.name()).isEqualTo(name);
        assertThat(acc.balance()).isEqualByComparingTo("0");
        assertThat(acc.currency()).isEqualTo("RUB");
    }

    @Test
    @Tag("smoke")
    @Story("US-02 Открытие счёта")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-03 Создание счёта с начальным остатком создаёт операцию OPENING")
    void createWithBalanceCreatesOpening() {
        Account acc = anAccountOfDemo().withBalance("1234.56").create();

        assertThat(acc.balance()).isEqualByComparingTo("1234.56");
        List<Operation> ops = operations.byAccount(acc.id());
        assertThat(ops).hasSize(1);
        Operation op = ops.get(0);
        assertThat(op.type()).isEqualTo("OPENING");
        assertThat(op.toAccountId()).isEqualTo(acc.id());
        assertThat(op.amount()).isEqualByComparingTo("1234.56");
        assertThat(op.fromAccountId()).isNull();
    }

    @Test
    @Story("US-06 Дневной лимит")
    @DisplayName("AT-04 GET /api/accounts/{id} возвращает счёт и дневной лимит")
    void getAccountWithDailyLimit() {
        Account acc = anAccountOfDemo().withBalance(500).create();

        AccountDetails details = accounts.get(acc.id());

        assertThat(details.id()).isEqualTo(acc.id());
        assertThat(details.balance()).isEqualByComparingTo("500");
        assertThat(details.dailyLimit()).isEqualByComparingTo("100000");
        assertThat(details.dailyTransferred()).isEqualByComparingTo("0");
        assertThat(details.dailyLimitRemaining()).isEqualByComparingTo("100000");
    }

    static Stream<Arguments> invalidNames() {
        return Stream.of(
                Arguments.of("пустое название", Map.of("name", "")),
                Arguments.of("название из пробелов", Map.of("name", "   ")),
                Arguments.of("название длиннее 50 символов (51)", Map.of("name", "a".repeat(51))),
                Arguments.of("без поля name", Map.of()));
    }

    @ParameterizedTest(name = "AT-05 Валидация: {0} — 400 VALIDATION_ERROR")
    @MethodSource("invalidNames")
    @Story("US-02 Открытие счёта")
    @DisplayName("AT-05 Валидация названия счёта")
    void invalidNameIsRejected(String title, Map<String, Object> body) {
        assertError(accounts.createRaw(body), 400, "VALIDATION_ERROR");
    }

    @Test
    @Story("US-02 Открытие счёта")
    @DisplayName("AT-06 Название ровно из 50 символов допустимо")
    void nameOf50CharsIsAccepted() {
        Response res = accounts.createRaw(new CreateAccountRequest("Я".repeat(50), null));
        assertThat(res.statusCode()).isEqualTo(201);
    }

    static Stream<Arguments> invalidInitialBalances() {
        return Stream.of(Arguments.of(-1), Arguments.of("10.123"), Arguments.of("abc"));
    }

    @ParameterizedTest(name = "AT-07 Некорректный начальный остаток {0}: 400 INVALID_AMOUNT")
    @MethodSource("invalidInitialBalances")
    @Story("US-02 Открытие счёта")
    @DisplayName("AT-07 Некорректный начальный остаток")
    void invalidInitialBalance(Object initialBalance) {
        Response res = accounts.createRaw(new CreateAccountRequest(TestData.uniqueName(), initialBalance));
        assertError(res, 400, "INVALID_AMOUNT");
    }

    @Test
    @DisplayName("AT-08 Несуществующий счёт: 404 ACCOUNT_NOT_FOUND")
    @Story("US-01 Просмотр счетов")
    void unknownAccount() {
        var error = assertError(accounts.getRaw(999999), 404, "ACCOUNT_NOT_FOUND");
        assertThat(error.message()).contains("999999");
    }

    @Test
    @DisplayName("AT-09 Некорректный id: 400 VALIDATION_ERROR")
    @Story("US-01 Просмотр счетов")
    void invalidId() {
        assertError(accounts.getRaw("abc"), 400, "VALIDATION_ERROR");
    }

    @Test
    @DisplayName("AT-10 Некорректный JSON: 400 INVALID_JSON")
    @Story("US-02 Открытие счёта")
    void invalidJson() {
        assertError(accounts.createRawString("{\"name\": "), 400, "INVALID_JSON");
    }
}
