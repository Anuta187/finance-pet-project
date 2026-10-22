package ru.oleynikova.finance.autotests.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.oleynikova.finance.autotests.assertions.ApiAssertions.assertError;
import static ru.oleynikova.finance.autotests.data.AccountBuilder.anAccount;

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

import ru.oleynikova.finance.autotests.api.client.CardsClient;
import ru.oleynikova.finance.autotests.api.client.PaymentsClient;
import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.api.model.Card;
import ru.oleynikova.finance.autotests.api.model.Category;
import ru.oleynikova.finance.autotests.api.model.Operation;
import ru.oleynikova.finance.autotests.api.model.PaymentRequest;
import ru.oleynikova.finance.autotests.api.model.PaymentResult;
import ru.oleynikova.finance.autotests.api.model.Spending;
import ru.oleynikova.finance.autotests.data.Session;
import ru.oleynikova.finance.autotests.data.Users;
import ru.oleynikova.finance.autotests.tests.BaseTest;

@Tag("api")
@Epic("Личные финансы")
@Feature("API: оплата услуг")
@DisplayName("API: оплата услуг и аналитика расходов")
class PaymentsApiTest extends BaseTest {

    private final PaymentsClient payments = new PaymentsClient(demo);

    @Test
    @Story("US-16 Оплата услуг")
    @DisplayName("AT-47 Справочник категорий: 8 категорий в фиксированном порядке")
    void categories() {
        List<Category> list = payments.categories();

        assertThat(list).extracting(Category::code).containsExactly(
                "MOBILE", "INTERNET", "UTILITIES", "TRANSPORT", "EDUCATION", "ENTERTAINMENT", "HEALTH", "OTHER");
        assertThat(list).allSatisfy(c -> assertThat(c.color()).matches("#[0-9a-fA-F]{6}"));
    }

    @Test
    @Tag("smoke")
    @Story("US-16 Оплата услуг")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("AT-48 Оплата со счёта: 201, операция PAYMENT с категорией, баланс уменьшается")
    void payFromAccount() {
        Account account = anAccountOfDemo().withBalance(1000).create();
        Category mobile = payments.category("MOBILE");

        PaymentResult res = payments.pay(new PaymentRequest(mobile.id(), account.id(), null, "350.50", "+7 900 000-00-00"));

        assertThat(res.operation().type()).isEqualTo("PAYMENT");
        assertThat(res.operation().categoryId()).isEqualTo(mobile.id());
        assertThat(res.operation().categoryName()).isEqualTo(mobile.name());
        assertThat(res.operation().fromAccountId()).isEqualTo(account.id());
        assertThat(res.operation().toAccountId()).isNull();
        assertThat(res.operation().cardId()).isNull();
        assertThat(res.account().balance()).isEqualByComparingTo("649.50");
        Operation inHistory = operations.byAccount(account.id()).get(0);
        assertThat(inHistory.id()).isEqualTo(res.operation().id());
        assertThat(inHistory.comment()).isEqualTo("+7 900 000-00-00");
    }

    @Test
    @Story("US-16 Оплата услуг")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-49 Оплата картой списывает деньги со счёта карты")
    void payByCard() {
        Account account = anAccountOfDemo().withBalance(500).create();
        Card card = new CardsClient(demo).issue(account.id());

        PaymentResult res = payments.pay(PaymentRequest.byCard(payments.category("TRANSPORT").id(), card.id(), 120));

        assertThat(res.operation().cardId()).isEqualTo(card.id());
        assertThat(res.operation().cardMasked()).isEqualTo(card.maskedNumber());
        assertThat(res.operation().fromAccountId()).isEqualTo(account.id());
        assertThat(accounts.get(account.id()).balance()).isEqualByComparingTo("380");
    }

    @Test
    @Story("US-16 Оплата услуг")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-50 Весь остаток списать можно, ещё 0.01 уже нет")
    void insufficientFunds() {
        Account account = anAccountOfDemo().withBalance(100).create();
        long health = payments.category("HEALTH").id();

        payments.pay(PaymentRequest.fromAccount(health, account.id(), 100));
        assertError(payments.payRaw(PaymentRequest.fromAccount(health, account.id(), 0.01)), 422, "INSUFFICIENT_FUNDS");
        assertThat(accounts.get(account.id()).balance()).isEqualByComparingTo("0");
    }

    static Stream<Arguments> invalidPayments() {
        return Stream.of(
                Arguments.of("нет источника", Map.of("categoryId", 1, "amount", 10), 400, "VALIDATION_ERROR"),
                Arguments.of("и счёт, и карта", Map.of("categoryId", 1, "accountId", 1, "cardId", 1, "amount", 10), 400, "VALIDATION_ERROR"),
                Arguments.of("нет категории", Map.of("accountId", 1, "amount", 10), 400, "VALIDATION_ERROR"),
                Arguments.of("сумма 0", Map.of("categoryId", 1, "accountId", 1, "amount", 0), 400, "INVALID_AMOUNT"),
                Arguments.of("сумма 3 знака после запятой", Map.of("categoryId", 1, "accountId", 1, "amount", "1.001"), 400, "INVALID_AMOUNT"),
                Arguments.of("несуществующая категория", Map.of("categoryId", 999, "accountId", 1, "amount", 10), 404, "CATEGORY_NOT_FOUND"));
    }

    @ParameterizedTest(name = "AT-51 Валидация платежа: {0} — {2} {3}")
    @MethodSource("invalidPayments")
    @Story("US-16 Оплата услуг")
    @DisplayName("AT-51 Валидация платежа")
    void invalidPayment(String title, Map<String, Object> body, int status, String code) {
        // счёт 1 — «Основной» демо-клиента после reset
        assertError(payments.payRaw(body), status, code);
    }

    @Test
    @Story("US-17 Аналитика расходов")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-52 Расходы по категориям за месяц: суммы, количество, сортировка по убыванию")
    void spendingStats() {
        Session client = Users.newClient();
        PaymentsClient clientPayments = new PaymentsClient(client);
        Account account = anAccount(client).withBalance(5000).create();
        long mobile = clientPayments.category("MOBILE").id();
        long utilities = clientPayments.category("UTILITIES").id();

        clientPayments.pay(PaymentRequest.fromAccount(mobile, account.id(), 300));
        clientPayments.pay(PaymentRequest.fromAccount(mobile, account.id(), 200.5));
        clientPayments.pay(PaymentRequest.fromAccount(utilities, account.id(), 1500));

        Spending spending = clientPayments.spending();
        assertThat(spending.month()).isEqualTo(service.today().toString().substring(0, 7));
        assertThat(spending.total()).isEqualByComparingTo("2000.50");
        assertThat(spending.categories()).extracting(Spending.Item::code).containsExactly("UTILITIES", "MOBILE");
        assertThat(spending.categories().get(1).amount()).isEqualByComparingTo("500.50");
        assertThat(spending.categories().get(1).count()).isEqualTo(2);
    }

    @Test
    @Story("US-06 Дневной лимит")
    @DisplayName("AT-53 Оплата услуг не расходует дневной лимит переводов")
    void paymentsDoNotUseTransferLimit() {
        Account account = anAccountOfDemo().withBalance(300000).create();

        payments.pay(PaymentRequest.fromAccount(payments.category("OTHER").id(), account.id(), 150000));

        assertThat(accounts.get(account.id()).dailyTransferred()).isEqualByComparingTo("0");
        assertThat(accounts.get(account.id()).dailyLimitRemaining()).isEqualByComparingTo("100000");
    }
}
