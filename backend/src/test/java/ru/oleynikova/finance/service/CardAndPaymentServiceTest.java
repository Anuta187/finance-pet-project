package ru.oleynikova.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import ru.oleynikova.finance.dto.PaymentRequest;
import ru.oleynikova.finance.dto.PaymentResult;
import ru.oleynikova.finance.entity.Account;
import ru.oleynikova.finance.entity.Card;
import ru.oleynikova.finance.entity.CardStatus;
import ru.oleynikova.finance.entity.Category;
import ru.oleynikova.finance.entity.Operation;
import ru.oleynikova.finance.entity.OperationType;
import ru.oleynikova.finance.entity.Role;
import ru.oleynikova.finance.entity.User;
import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.error.ErrorCode;
import ru.oleynikova.finance.repository.AccountRepository;
import ru.oleynikova.finance.repository.CardRepository;
import ru.oleynikova.finance.repository.CategoryRepository;
import ru.oleynikova.finance.repository.OperationRepository;
import ru.oleynikova.finance.support.Translit;

@ExtendWith(MockitoExtension.class)
@DisplayName("Карты и платежи")
class CardAndPaymentServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T05:00:00Z"), ZoneId.of("Asia/Vladivostok"));
    private static final long USER_ID = 7L;

    @Mock
    private AccountRepository accounts;
    @Mock
    private OperationRepository operations;
    @Mock
    private CategoryRepository categories;
    @Mock
    private CardRepository cards;

    private CardService cardService;
    private PaymentService payments;
    private Account account;
    private Category mobile;

    @BeforeEach
    void setUp() {
        cardService = new CardService(cards, accounts, CLOCK);
        payments = new PaymentService(accounts, operations, categories, cardService, CLOCK);
        User owner = new User("u@test.ru", "hash", "Иван Петров", Role.CLIENT, CLOCK.instant());
        owner.setId(USER_ID);
        account = new Account(owner, "Основной", 100_000, CLOCK.instant());   // 1 000 ₽
        account.setId(1L);
        mobile = new Category(1L, "MOBILE", "Мобильная связь", "#6366F1");
    }

    @RepeatedTest(5)
    @DisplayName("Номер карты: 16 цифр, БИН 2200, корректная контрольная сумма Луна")
    void cardNumberIsLuhnValid() {
        String number = cardService.generateNumber();

        assertThat(number).hasSize(16).startsWith("2200").containsOnlyDigits();
        int sum = 0;
        for (int i = 0; i < 16; i++) {
            int d = number.charAt(15 - i) - '0';
            if (i % 2 == 1) {
                d = d * 2 > 9 ? d * 2 - 9 : d * 2;
            }
            sum += d;
        }
        assertThat(sum % 10).isZero();
    }

    @Test
    @DisplayName("Новая карта: маска «2200 **** **** XXXX», срок 4 года, статус ACTIVE, держатель латиницей")
    void newCard() {
        Card card = cardService.newCard(account, Translit.cardHolder("Иван Петров"));

        assertThat(card.maskedNumber()).matches("2200 \\*{4} \\*{4} \\d{4}");
        assertThat(card.getExpiryYear()).isEqualTo(2030);
        assertThat(card.getExpiryMonth()).isEqualTo(10);
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(card.getHolderName()).isEqualTo("IVAN PETROV");
        assertThat(card.isExpiredAt(YearMonth.of(2030, 10))).isFalse();
        assertThat(card.isExpiredAt(YearMonth.of(2030, 11))).isTrue();
    }

    @Test
    @DisplayName("Платёж по карте списывает деньги со счёта карты и пишет PAYMENT с категорией и картой")
    void payByCard() {
        Card card = card(CardStatus.ACTIVE, YearMonth.of(2030, 10));
        when(categories.findById(1L)).thenReturn(Optional.of(mobile));
        when(cards.findById(5L)).thenReturn(Optional.of(card));
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(operations.save(any(Operation.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentResult result = payments.pay(USER_ID, new PaymentRequest(IntNode.valueOf(1), null, IntNode.valueOf(5),
                TextNode.valueOf("650,50"), " +7 914 "));

        assertThat(account.getBalanceKop()).isEqualTo(34_950);
        assertThat(result.operation().type()).isEqualTo(OperationType.PAYMENT);
        assertThat(result.operation().categoryName()).isEqualTo("Мобильная связь");
        assertThat(result.operation().cardId()).isEqualTo(5L);
        assertThat(result.operation().toAccountId()).isNull();
        assertThat(result.operation().comment()).isEqualTo("+7 914");
    }

    @Test
    @DisplayName("Заблокированная карта: CARD_BLOCKED, счёт не блокируется и не списывается")
    void blockedCard() {
        when(categories.findById(1L)).thenReturn(Optional.of(mobile));
        when(cards.findById(5L)).thenReturn(Optional.of(card(CardStatus.BLOCKED, YearMonth.of(2030, 10))));

        assertThatThrownBy(() -> payments.pay(USER_ID, request(null, 5, "10")))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.CARD_BLOCKED);
        assertThat(account.getBalanceKop()).isEqualTo(100_000);
        verify(operations, never()).save(any());
    }

    @Test
    @DisplayName("Просроченная карта: CARD_EXPIRED")
    void expiredCard() {
        when(categories.findById(1L)).thenReturn(Optional.of(mobile));
        when(cards.findById(5L)).thenReturn(Optional.of(card(CardStatus.ACTIVE, YearMonth.of(2026, 9))));

        assertThatThrownBy(() -> payments.pay(USER_ID, request(null, 5, "10")))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.CARD_EXPIRED);
    }

    @Test
    @DisplayName("Оба источника или ни одного: VALIDATION_ERROR по полю source")
    void exactlyOneSource() {
        assertThatThrownBy(() -> payments.pay(USER_ID, request(1, 5, "10")))
                .satisfies(e -> assertThat(((ApiException) e).getDetails()).containsEntry("field", "source"));
        assertThatThrownBy(() -> payments.pay(USER_ID, request(null, null, "10")))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("Сумма больше остатка: INSUFFICIENT_FUNDS; несуществующая категория: CATEGORY_NOT_FOUND")
    void insufficientFundsAndUnknownCategory() {
        when(categories.findById(1L)).thenReturn(Optional.of(mobile));
        when(categories.findById(99L)).thenReturn(Optional.empty());
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> payments.pay(USER_ID, request(1, null, "1000.01")))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
        assertThatThrownBy(() -> payments.pay(USER_ID, new PaymentRequest(IntNode.valueOf(99), IntNode.valueOf(1), null,
                TextNode.valueOf("1"), null)))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.CATEGORY_NOT_FOUND);
        assertThat(account.getBalanceKop()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("Карта другого пользователя: CARD_NOT_FOUND")
    void foreignCard() {
        User stranger = new User("x@test.ru", "hash", "Чужой", Role.CLIENT, CLOCK.instant());
        stranger.setId(99L);
        Account foreign = new Account(stranger, "Чужой", 100_000, CLOCK.instant());
        Card card = new Card(foreign, "2200", "0000", "МИР", "X", YearMonth.of(2030, 1), CLOCK.instant());
        when(cards.findById(5L)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.setStatus(USER_ID, "5", CardStatus.BLOCKED))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.CARD_NOT_FOUND);
        assertThat(card.getStatus()).isEqualTo(CardStatus.ACTIVE);
    }

    private Card card(CardStatus status, YearMonth expiry) {
        Card c = new Card(account, "2200", "1234", "МИР", "IVAN PETROV", expiry, CLOCK.instant());
        c.setId(5L);
        c.setStatus(status);
        return c;
    }

    private static PaymentRequest request(Integer accountId, Integer cardId, String amount) {
        return new PaymentRequest(IntNode.valueOf(1), accountId == null ? null : IntNode.valueOf(accountId),
                cardId == null ? null : IntNode.valueOf(cardId), TextNode.valueOf(amount), null);
    }
}
