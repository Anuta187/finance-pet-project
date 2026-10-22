package ru.oleynikova.finance.autotests.tests.api;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.oleynikova.finance.autotests.assertions.ApiAssertions.assertError;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import io.restassured.response.Response;

import ru.oleynikova.finance.autotests.api.client.CardsClient;
import ru.oleynikova.finance.autotests.api.client.PaymentsClient;
import ru.oleynikova.finance.autotests.api.model.Account;
import ru.oleynikova.finance.autotests.api.model.Card;
import ru.oleynikova.finance.autotests.api.model.PaymentRequest;
import ru.oleynikova.finance.autotests.tests.BaseTest;

@Tag("api")
@Epic("Личные финансы")
@Feature("API: банковские карты")
@Story("US-15 Карты")
@DisplayName("API: банковские карты")
class CardsApiTest extends BaseTest {

    private final CardsClient cards = new CardsClient(demo);
    private final PaymentsClient payments = new PaymentsClient(demo);

    @Test
    @Tag("smoke")
    @Severity(SeverityLevel.CRITICAL)
    @DisplayName("AT-44 Выпуск карты: маскированный номер, срок +4 года, держатель латиницей, статус ACTIVE")
    void issueCard() {
        Account account = anAccountOfDemo().withBalance(100).create();
        LocalDate today = service.today();

        Response raw = cards.issueRaw(account.id());
        assertThat(raw.statusCode()).isEqualTo(201);
        Card card = raw.as(Card.class);

        assertThat(card.maskedNumber()).matches("2200 \\*{4} \\*{4} \\d{4}");
        assertThat(card.maskedNumber()).endsWith(card.last4());
        assertThat(card.paymentSystem()).isEqualTo("МИР");
        assertThat(card.holderName()).isEqualTo("IVAN PETROV");
        assertThat(card.status()).isEqualTo("ACTIVE");
        assertThat(card.accountId()).isEqualTo(account.id());
        assertThat(card.expiryMonth()).isEqualTo(today.getMonthValue());
        assertThat(card.expiryYear()).isEqualTo(today.getYear() + 4);
        assertThat(card.expiry()).isEqualTo(String.format("%02d/%02d", today.getMonthValue(), (today.getYear() + 4) % 100));
        // в ответе нет полного номера
        assertThat(raw.asString()).doesNotContainPattern("\\d{16}");
        assertThat(cards.list()).extracting(Card::id).contains(card.id());
    }

    @Test
    @Severity(SeverityLevel.BLOCKER)
    @DisplayName("AT-45 Заблокированной картой нельзя платить, после разблокировки можно")
    void blockedCardCannotPay() {
        Account account = anAccountOfDemo().withBalance(1000).create();
        Card card = cards.issue(account.id());
        long internet = payments.category("INTERNET").id();

        assertThat(cards.block(card.id()).status()).isEqualTo("BLOCKED");
        assertError(payments.payRaw(PaymentRequest.byCard(internet, card.id(), 100)), 422, "CARD_BLOCKED");
        assertThat(accounts.get(account.id()).balance()).isEqualByComparingTo("1000");

        assertThat(cards.unblock(card.id()).status()).isEqualTo("ACTIVE");
        payments.pay(PaymentRequest.byCard(internet, card.id(), 100));
        assertThat(accounts.get(account.id()).balance()).isEqualByComparingTo("900");
    }

    @Test
    @DisplayName("AT-46 Ошибки при выпуске и блокировке карты")
    void issueNegative() {
        assertError(cards.issueRaw(999999), 404, "ACCOUNT_NOT_FOUND");
        assertError(cards.issueRaw(null), 400, "VALIDATION_ERROR");
        assertError(cards.blockRaw(999999), 404, "CARD_NOT_FOUND");
        assertError(cards.unblockRaw("abc"), 400, "VALIDATION_ERROR");
    }
}
