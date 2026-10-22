package ru.oleynikova.finance.autotests.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.oleynikova.finance.autotests.assertions.ApiAssertions.assertError;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import ru.oleynikova.finance.autotests.api.model.Operation;
import ru.oleynikova.finance.autotests.api.model.TransferRequest;
import ru.oleynikova.finance.autotests.api.model.TransferResult;
import ru.oleynikova.finance.autotests.tests.BaseTest;

@Tag("api")
@Epic("Личные финансы")
@Feature("API: переводы")
@DisplayName("API: переводы")
class TransfersApiTest extends BaseTest {

    private BigDecimal balanceOf(Account account) {
        return accounts.get(account.id()).balance();
    }

    @Nested
    @DisplayName("Позитивные сценарии")
    class Positive {

        @Test
        @Tag("smoke")
        @Story("US-03 Перевод между счетами")
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("AT-11 Успешный перевод меняет оба баланса и пишет операцию")
        void successfulTransfer() {
            Account from = anAccountOfDemo().withBalance(1000).create();
            Account to = anAccountOfDemo().withBalance(0).create();

            Response res = transfers.transferRaw(new TransferRequest(from.id(), to.id(), 250.75, "Тест"));

            assertThat(res.statusCode()).isEqualTo(201);
            TransferResult body = res.as(TransferResult.class);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(body.operation().type()).isEqualTo("TRANSFER");
                soft.assertThat(body.operation().fromAccountId()).isEqualTo(from.id());
                soft.assertThat(body.operation().toAccountId()).isEqualTo(to.id());
                soft.assertThat(body.operation().amount()).isEqualByComparingTo("250.75");
                soft.assertThat(body.operation().comment()).isEqualTo("Тест");
                soft.assertThat(body.fromAccount().balance()).isEqualByComparingTo("749.25");
                soft.assertThat(body.toAccount().balance()).isEqualByComparingTo("250.75");
                soft.assertThat(balanceOf(from)).isEqualByComparingTo("749.25");
                soft.assertThat(balanceOf(to)).isEqualByComparingTo("250.75");
            });
            Operation last = operations.byAccount(to.id()).get(0);
            assertThat(last.id()).isEqualTo(body.operation().id());
            assertThat(last.type()).isEqualTo("TRANSFER");
        }

        @Test
        @Story("US-04 Проверка суммы")
        @DisplayName("AT-12 Граница: минимальная сумма 0.01")
        void minimalAmount() {
            Account from = anAccountOfDemo().withBalance(1).create();
            Account to = anAccountOfDemo().create();

            transfers.transfer(from.id(), to.id(), 0.01);

            assertThat(balanceOf(from)).isEqualByComparingTo("0.99");
        }

        @Test
        @Story("US-05 Проверка остатка")
        @DisplayName("AT-13 Граница: перевод всего остатка")
        void wholeBalance() {
            Account from = anAccountOfDemo().withBalance(300.3).create();
            Account to = anAccountOfDemo().create();

            transfers.transfer(from.id(), to.id(), 300.3);

            assertThat(balanceOf(from)).isEqualByComparingTo("0");
        }

        @Test
        @Story("US-03 Перевод между счетами")
        @DisplayName("AT-14 Сумма строкой с запятой \"10,50\" принимается")
        void amountWithComma() {
            Account from = anAccountOfDemo().withBalance(100).create();
            Account to = anAccountOfDemo().create();

            TransferResult result = transfers.transfer(from.id(), to.id(), "10,50");

            assertThat(result.operation().amount()).isEqualByComparingTo("10.5");
        }

        @Test
        @Severity(SeverityLevel.CRITICAL)
        @Story("NFR-2 Точность расчётов")
        @DisplayName("AT-15 Точность: 0.1 + 0.2 без ошибок округления")
        void precision() {
            Account from = anAccountOfDemo().withBalance(1).create();
            Account to = anAccountOfDemo().create();

            transfers.transfer(from.id(), to.id(), 0.1);
            transfers.transfer(from.id(), to.id(), 0.2);

            assertThat(balanceOf(to)).isEqualByComparingTo("0.3");
            assertThat(balanceOf(from)).isEqualByComparingTo("0.7");
        }
    }

    @Nested
    @DisplayName("Негативные сценарии")
    class Negative {

        static Stream<Arguments> invalidAmounts() {
            return Stream.of(
                    Arguments.of(0), Arguments.of(-100), Arguments.of(10.123),
                    Arguments.of("abc"), Arguments.of("1e3"), Arguments.of(true));
        }

        @ParameterizedTest(name = "AT-16 Некорректная сумма {0}: 400 INVALID_AMOUNT")
        @MethodSource("invalidAmounts")
        @Story("US-04 Проверка суммы")
        @Severity(SeverityLevel.CRITICAL)
        @DisplayName("AT-16 Некорректная сумма")
        void invalidAmount(Object amount) {
            Account from = anAccountOfDemo().withBalance(1000).create();
            Account to = anAccountOfDemo().create();

            assertError(transfers.transferRaw(from.id(), to.id(), amount), 400, "INVALID_AMOUNT");
            assertThat(balanceOf(from)).isEqualByComparingTo("1000");
        }

        @Test
        @Story("US-03 Перевод между счетами")
        @DisplayName("AT-17 Отсутствуют обязательные поля или некорректный id: 400 VALIDATION_ERROR")
        void missingFields() {
            List<Map<String, Object>> bodies = List.of(
                    Map.of(),
                    Map.of("fromAccountId", 1, "toAccountId", 2),
                    Map.of("toAccountId", 2, "amount", 10),
                    Map.of("fromAccountId", "x", "toAccountId", 2, "amount", 1));
            for (Map<String, Object> body : bodies) {
                assertError(transfers.transferRaw(body), 400, "VALIDATION_ERROR");
            }
        }

        @Test
        @Story("US-06 Ограничения перевода")
        @DisplayName("AT-18 Перевод на тот же счёт: 422 SAME_ACCOUNT")
        void sameAccount() {
            Account acc = anAccountOfDemo().withBalance(100).create();

            assertError(transfers.transferRaw(acc.id(), acc.id(), 10), 422, "SAME_ACCOUNT");
            assertThat(balanceOf(acc)).isEqualByComparingTo("100");
        }

        @Test
        @Story("US-05 Проверка остатка")
        @Severity(SeverityLevel.BLOCKER)
        @DisplayName("AT-19 Остаток + 0.01: 422 INSUFFICIENT_FUNDS, балансы не меняются")
        void insufficientFunds() {
            Account from = anAccountOfDemo().withBalance(100).create();
            Account to = anAccountOfDemo().withBalance(5).create();

            var error = assertError(transfers.transferRaw(from.id(), to.id(), 100.01), 422, "INSUFFICIENT_FUNDS");

            assertThat(new BigDecimal(error.details().get("balance").toString())).isEqualByComparingTo("100");
            assertThat(new BigDecimal(error.details().get("amount").toString())).isEqualByComparingTo("100.01");
            assertThat(balanceOf(from)).isEqualByComparingTo("100");
            assertThat(balanceOf(to)).isEqualByComparingTo("5");
        }

        @Test
        @Story("US-03 Перевод между счетами")
        @DisplayName("AT-20 Несуществующий счёт: 404 ACCOUNT_NOT_FOUND, транзакция откатывается")
        void unknownAccount() {
            Account acc = anAccountOfDemo().withBalance(100).create();

            assertError(transfers.transferRaw(acc.id(), 999999, 1), 404, "ACCOUNT_NOT_FOUND");
            assertError(transfers.transferRaw(999999, acc.id(), 1), 404, "ACCOUNT_NOT_FOUND");
            assertThat(balanceOf(acc)).isEqualByComparingTo("100");
        }
    }
}
