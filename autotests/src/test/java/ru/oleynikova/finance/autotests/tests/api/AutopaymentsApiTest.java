package ru.oleynikova.finance.autotests.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.oleynikova.finance.autotests.assertions.ApiAssertions.assertError;
import static ru.oleynikova.finance.autotests.data.AccountBuilder.anAccount;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;

import ru.oleynikova.finance.autotests.api.client.AccountsClient;
import ru.oleynikova.finance.autotests.api.client.AdminClient;
import ru.oleynikova.finance.autotests.api.client.AutopaymentsClient;
import ru.oleynikova.finance.autotests.api.client.CardsClient;
import ru.oleynikova.finance.autotests.api.client.OperationsClient;
import ru.oleynikova.finance.autotests.api.client.PaymentsClient;
import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.api.model.Autopayment;
import ru.oleynikova.finance.autotests.api.model.AutopaymentRunResult;
import ru.oleynikova.finance.autotests.api.model.Card;
import ru.oleynikova.finance.autotests.api.model.CreateAutopaymentRequest;
import ru.oleynikova.finance.autotests.api.model.Operation;
import ru.oleynikova.finance.autotests.data.Session;
import ru.oleynikova.finance.autotests.data.Users;
import ru.oleynikova.finance.autotests.tests.BaseTest;

// Планировщик в профиле test выключен, автоплатежи запускаются через админский API
@Tag("api")
@Epic("Личные финансы")
@Feature("API: автоплатежи")
@Story("US-18 Автоплатежи")
@DisplayName("API: автоплатежи")
class AutopaymentsApiTest extends BaseTest {

    private Session client;
    private AutopaymentsClient autopayments;
    private AccountsClient clientAccounts;
    private AdminClient admin;
    private long mobileId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        client = Users.newClient();
        autopayments = new AutopaymentsClient(client);
        clientAccounts = new AccountsClient(client);
        admin = new AdminClient(Users.admin());
        mobileId = new PaymentsClient(client).category("MOBILE").id();
        today = service.today();
    }

    @AfterEach
    void restoreClock() {
        service.resetClock();
    }

    private Autopayment monthly(Account account, Object amount, LocalDate start) {
        return autopayments.create(CreateAutopaymentRequest.monthlyFromAccount(
                "Связь", mobileId, account.id(), amount, start == null ? null : start.toString()));
    }

    private AutopaymentRunResult.Item run(Autopayment ap) {
        return admin.runAutopayments().itemFor(ap.id())
                .orElseThrow(() -> new AssertionError("Автоплатёж " + ap.id() + " не был исполнен"));
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-54 Создание: по умолчанию первое списание сегодня, автоплатёж активен")
    void create() {
        Account account = anAccount(client).withBalance(1000).create();

        Autopayment ap = monthly(account, "650", null);

        assertThat(ap.name()).isEqualTo("Связь");
        assertThat(ap.period()).isEqualTo("MONTHLY");
        assertThat(ap.startDate()).isEqualTo(today.toString());
        assertThat(ap.nextRunDate()).isEqualTo(today.toString());
        assertThat(ap.active()).isTrue();
        assertThat(ap.lastStatus()).isNull();
        assertThat(ap.amount()).isEqualByComparingTo("650");
        assertThat(autopayments.list()).extracting(Autopayment::id).containsExactly(ap.id());
    }

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("AT-55 Исполнение в срок: списание, операция с autopaymentId, перенос на месяц; повторный запуск не списывает")
    void runDue() {
        Account account = anAccount(client).withBalance(1000).create();
        Autopayment ap = monthly(account, "650", null);

        AutopaymentRunResult.Item item = run(ap);

        assertThat(item.status()).isEqualTo("SUCCESS");
        assertThat(item.operationId()).isNotNull();
        assertThat(clientAccounts.get(account.id()).balance()).isEqualByComparingTo("350");
        Operation op = new OperationsClient(client).byAccount(account.id()).get(0);
        assertThat(op.type()).isEqualTo("PAYMENT");
        assertThat(op.autopaymentId()).isEqualTo(ap.id());
        assertThat(op.comment()).isEqualTo("Автоплатёж: Связь");

        Autopayment after = autopayments.get(ap.id());
        assertThat(after.lastStatus()).isEqualTo("SUCCESS");
        assertThat(after.nextRunDate()).isEqualTo(today.plusMonths(1).toString());

        assertThat(admin.runAutopayments().itemFor(ap.id())).isEmpty();
        assertThat(clientAccounts.get(account.id()).balance()).isEqualByComparingTo("350");
    }

    @ParameterizedTest(name = "AT-56 Период {0}: следующая дата через {1} дн.")
    @CsvSource({"DAILY, 1", "WEEKLY, 7"})
    @DisplayName("AT-56 Расчёт следующей даты для DAILY и WEEKLY")
    void nextDateByPeriod(String period, int days) {
        Account account = anAccount(client).withBalance(100).create();
        Autopayment ap = autopayments.create(new CreateAutopaymentRequest("Тест", mobileId, account.id(), null,
                1, period, null));

        run(ap);

        assertThat(autopayments.get(ap.id()).nextRunDate()).isEqualTo(today.plusDays(days).toString());
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-57 Автоплатёж с будущей датой не исполняется раньше срока")
    void futureStartDate() {
        Account account = anAccount(client).withBalance(1000).create();
        Autopayment ap = monthly(account, 100, today.plusDays(3));

        assertThat(admin.runAutopayments().itemFor(ap.id())).isEmpty();

        service.shiftClock(2);
        assertThat(admin.runAutopayments().itemFor(ap.id())).isEmpty();

        service.shiftClock(1);
        assertThat(run(ap).status()).isEqualTo("SUCCESS");
        assertThat(clientAccounts.get(account.id()).balance()).isEqualByComparingTo("900");
    }

    @Test
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-58 Недостаточно средств: FAILED с причиной, деньги не списаны, дата переносится")
    void insufficientFunds() {
        Account account = anAccount(client).withBalance(100).create();
        Autopayment ap = monthly(account, 650, null);

        AutopaymentRunResult.Item item = run(ap);

        assertThat(item.status()).isEqualTo("FAILED");
        assertThat(item.errorCode()).isEqualTo("INSUFFICIENT_FUNDS");
        Autopayment after = autopayments.get(ap.id());
        assertThat(after.lastStatus()).isEqualTo("FAILED");
        assertThat(after.lastError()).startsWith("INSUFFICIENT_FUNDS");
        assertThat(after.nextRunDate()).isEqualTo(today.plusMonths(1).toString());
        assertThat(after.active()).isTrue();
        assertThat(clientAccounts.get(account.id()).balance()).isEqualByComparingTo("100");
    }

    @Test
    @DisplayName("AT-59 Карта заблокирована после создания автоплатежа: FAILED CARD_BLOCKED")
    void blockedCard() {
        Account account = anAccount(client).withBalance(1000).create();
        CardsClient cards = new CardsClient(client);
        Card card = cards.issue(account.id());
        Autopayment ap = autopayments.create(new CreateAutopaymentRequest("Интернет", mobileId, null, card.id(),
                300, "MONTHLY", null));
        assertThat(ap.cardMasked()).isEqualTo(card.maskedNumber());

        cards.block(card.id());

        assertThat(run(ap).errorCode()).isEqualTo("CARD_BLOCKED");
        assertThat(clientAccounts.get(account.id()).balance()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("AT-60 Отключённый автоплатёж и автоплатёж заблокированного клиента не исполняются")
    void disabledAndBlockedUser() {
        Account account = anAccount(client).withBalance(1000).create();
        Autopayment disabled = monthly(account, 100, null);
        Autopayment ofBlocked = monthly(account, 200, null);

        Autopayment off = autopayments.disableRaw(disabled.id()).then().statusCode(200).extract().as(Autopayment.class);
        assertThat(off.active()).isFalse();
        admin.block(client.userId());

        AutopaymentRunResult result = admin.runAutopayments();

        assertThat(result.itemFor(disabled.id())).isEmpty();
        assertThat(result.itemFor(ofBlocked.id())).get().satisfies(i -> assertThat(i.errorCode()).isEqualTo("USER_BLOCKED"));
        admin.unblock(client.userId());
        assertThat(clientAccounts.get(account.id()).balance()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("AT-61 Валидация автоплатежа")
    void validation() {
        Account account = anAccount(client).withBalance(100).create();
        long accId = account.id();

        assertError(autopayments.createRaw(Map.of("name", "A", "categoryId", mobileId, "accountId", accId, "amount", 1,
                "period", "MONTHLY", "startDate", today.minusDays(1).toString())), 400, "VALIDATION_ERROR");
        assertError(autopayments.createRaw(Map.of("name", "A", "categoryId", mobileId, "accountId", accId, "amount", 1,
                "period", "YEARLY")), 400, "VALIDATION_ERROR");
        assertError(autopayments.createRaw(Map.of("name", " ", "categoryId", mobileId, "accountId", accId, "amount", 1,
                "period", "MONTHLY")), 400, "VALIDATION_ERROR");
        assertError(autopayments.createRaw(Map.of("name", "A", "categoryId", mobileId, "accountId", accId, "amount", 0,
                "period", "MONTHLY")), 400, "INVALID_AMOUNT");
        assertError(autopayments.createRaw(Map.of("name", "A", "categoryId", mobileId, "accountId", 1, "amount", 1,
                "period", "MONTHLY")), 404, "ACCOUNT_NOT_FOUND");
        assertError(autopayments.disableRaw(999999), 404, "AUTOPAYMENT_NOT_FOUND");
        assertThat(autopayments.list()).isEmpty();
    }
}
