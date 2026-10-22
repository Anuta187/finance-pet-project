package ru.oleynikova.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import ru.oleynikova.finance.dto.AutopaymentRunResult;
import ru.oleynikova.finance.entity.Account;
import ru.oleynikova.finance.entity.Autopayment;
import ru.oleynikova.finance.entity.AutopaymentPeriod;
import ru.oleynikova.finance.entity.AutopaymentStatus;
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
import ru.oleynikova.finance.repository.AutopaymentRepository;
import ru.oleynikova.finance.repository.CardRepository;
import ru.oleynikova.finance.repository.CategoryRepository;
import ru.oleynikova.finance.repository.OperationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Автоплатежи: расписание и исполнение")
class AutopaymentServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Vladivostok");
    // "сегодня" 01.11.2026
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T01:00:00Z"), ZONE);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

    @Mock
    private AutopaymentRepository autopayments;
    @Mock
    private AccountRepository accounts;
    @Mock
    private OperationRepository operations;
    @Mock
    private CategoryRepository categories;
    @Mock
    private CardRepository cards;

    private AutopaymentService service;
    private User owner;
    private Account account;
    private Category mobile;

    @BeforeEach
    void setUp() {
        CardService cardService = new CardService(cards, accounts, CLOCK);
        PaymentService payments = new PaymentService(accounts, operations, categories, cardService, CLOCK);
        service = new AutopaymentService(autopayments, accounts, payments, cardService,
                mock(PlatformTransactionManager.class), CLOCK);
        owner = new User("u@test.ru", "hash", "Иван Петров", Role.CLIENT, CLOCK.instant());
        owner.setId(7L);
        account = new Account(owner, "Основной", 100_000, CLOCK.instant());
        account.setId(1L);
        mobile = new Category(1L, "MOBILE", "Мобильная связь", "#6366F1");
    }

    @ParameterizedTest(name = "{0}: старт {1}, запуск №{2}: {3}")
    @CsvSource({
            "MONTHLY, 2026-01-31, 1, 2026-02-28",
            "MONTHLY, 2026-01-31, 2, 2026-03-31",
            "MONTHLY, 2026-10-22, 1, 2026-11-22",
            "WEEKLY,  2026-10-22, 2, 2026-11-05",
            "DAILY,   2026-12-31, 1, 2027-01-01"})
    @DisplayName("Даты запусков считаются от даты старта, 31-е число не съезжает")
    void periodDates(AutopaymentPeriod period, LocalDate start, int n, LocalDate expected) {
        assertThat(period.nth(start, n)).isEqualTo(expected);
    }

    @Test
    @DisplayName("Успешное исполнение: списание, операция PAYMENT со ссылкой на автоплатёж, следующая дата через месяц")
    void processSuccess() {
        Autopayment ap = autopayment(null, TODAY);
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(operations.save(any(Operation.class))).thenAnswer(inv -> inv.getArgument(0));

        AutopaymentRunResult.Item item = service.process(ap, TODAY);

        assertThat(item.status()).isEqualTo(AutopaymentStatus.SUCCESS);
        assertThat(account.getBalanceKop()).isEqualTo(35_000);
        assertThat(ap.getNextRunDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        assertThat(ap.getLastStatus()).isEqualTo(AutopaymentStatus.SUCCESS);
        verify(operations).save(org.mockito.ArgumentMatchers.argThat(op ->
                op.getType() == OperationType.PAYMENT && op.getAutopayment() == ap
                        && "Автоплатёж: Мобильная связь".equals(op.getComment())));
    }

    @Test
    @DisplayName("Недостаточно средств: статус FAILED с кодом ошибки, деньги не списаны, дата сдвинута на следующий период")
    void processInsufficientFunds() {
        account.debit(99_000);   // осталось 10 ₽
        Autopayment ap = autopayment(null, TODAY);
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));

        AutopaymentRunResult.Item item = service.process(ap, TODAY);

        assertThat(item.status()).isEqualTo(AutopaymentStatus.FAILED);
        assertThat(item.errorCode()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(ap.getLastError()).startsWith("INSUFFICIENT_FUNDS");
        assertThat(account.getBalanceKop()).isEqualTo(1_000);
        assertThat(ap.getNextRunDate()).isEqualTo(LocalDate.of(2026, 12, 1));
        verify(operations, never()).save(any());
    }

    @Test
    @DisplayName("Автоплатёж по заблокированной карте: FAILED CARD_BLOCKED")
    void processBlockedCard() {
        Card card = new Card(account, "2200", "1234", "МИР", "IVAN PETROV", YearMonth.of(2030, 1), CLOCK.instant());
        card.setId(5L);
        card.setStatus(CardStatus.BLOCKED);
        when(cards.findById(5L)).thenReturn(Optional.of(card));

        AutopaymentRunResult.Item item = service.process(autopayment(card, TODAY), TODAY);

        assertThat(item.errorCode()).isEqualTo("CARD_BLOCKED");
        assertThat(account.getBalanceKop()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("Владелец заблокирован: FAILED USER_BLOCKED, списания нет")
    void processBlockedOwner() {
        owner.setBlocked(true);

        AutopaymentRunResult.Item item = service.process(autopayment(null, TODAY), TODAY);

        assertThat(item.errorCode()).isEqualTo("USER_BLOCKED");
        verify(accounts, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Пропущенные периоды не догоняются: одно списание, следующая дата — первая после сегодня")
    void missedPeriodsAreSkipped() {
        Autopayment ap = autopayment(null, LocalDate.of(2026, 8, 1));
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(account));
        when(operations.save(any(Operation.class))).thenAnswer(inv -> inv.getArgument(0));

        service.process(ap, TODAY);

        assertThat(account.getBalanceKop()).isEqualTo(35_000);
        assertThat(ap.getNextRunDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    @DisplayName("Дата начала в прошлом: VALIDATION_ERROR; без даты — сегодня; неверный период: VALIDATION_ERROR")
    void startDateAndPeriodValidation() {
        assertThatThrownBy(() -> AutopaymentService.parseStartDate("2026-10-31", TODAY))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(AutopaymentService.parseStartDate(null, TODAY)).isEqualTo(TODAY);
        assertThat(AutopaymentService.parseStartDate("2026-11-01", TODAY)).isEqualTo(TODAY);
        assertThat(AutopaymentService.parsePeriod("monthly")).isEqualTo(AutopaymentPeriod.MONTHLY);
        assertThatThrownBy(() -> AutopaymentService.parsePeriod("YEARLY"))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private Autopayment autopayment(Card card, LocalDate start) {
        Autopayment ap = new Autopayment(account, card, mobile, "Мобильная связь", 65_000, AutopaymentPeriod.MONTHLY,
                start, CLOCK.instant());
        ap.setId(3L);
        return ap;
    }
}
