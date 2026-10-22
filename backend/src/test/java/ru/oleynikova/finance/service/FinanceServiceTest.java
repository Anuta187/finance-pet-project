package ru.oleynikova.finance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.TextNode;

import ru.oleynikova.finance.dto.CreateAccountRequest;
import ru.oleynikova.finance.dto.TransferRequest;
import ru.oleynikova.finance.dto.TransferResult;
import ru.oleynikova.finance.entity.Account;
import ru.oleynikova.finance.entity.Operation;
import ru.oleynikova.finance.entity.OperationType;
import ru.oleynikova.finance.entity.Role;
import ru.oleynikova.finance.entity.User;
import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.error.ErrorCode;
import ru.oleynikova.finance.repository.AccountRepository;
import ru.oleynikova.finance.repository.OperationRepository;
import ru.oleynikova.finance.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinanceService: счета и переводы")
class FinanceServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Vladivostok");
    // 22.10.2026 15:00 по Владивостоку
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T05:00:00Z"), ZONE);
    // начало суток по Владивостоку
    private static final Instant START_OF_DAY = Instant.parse("2026-10-21T14:00:00Z");

    @Mock
    private AccountRepository accounts;
    @Mock
    private OperationRepository operations;
    @Mock
    private UserRepository users;

    private static final long OWNER_ID = 7L;
    private static final User OWNER = user(OWNER_ID);
    private static final User STRANGER = user(8L);

    private FinanceService service;
    private Account from;
    private Account to;

    @BeforeEach
    void setUp() {
        service = new FinanceService(accounts, operations, users, CLOCK, new BigDecimal("100000"));
        from = account(1L, "Основной", 100_000);   // 1 000,00 ₽
        to = account(2L, "Накопительный", 0);
    }

    @Test
    @DisplayName("Успешный перевод списывает и зачисляет сумму и пишет операцию TRANSFER")
    void transferMovesMoney() {
        givenLocked(from, to);
        when(operations.sumTransferredSince(1L, START_OF_DAY)).thenReturn(0L);
        when(operations.save(any(Operation.class))).thenAnswer(inv -> withId(inv.getArgument(0), 10L));

        TransferResult result = service.transfer(OWNER_ID, request(1, 2, "250,75", "  На отпуск  "));

        assertThat(from.getBalanceKop()).isEqualTo(74_925);
        assertThat(to.getBalanceKop()).isEqualTo(25_075);
        assertThat(result.operation().type()).isEqualTo(OperationType.TRANSFER);
        assertThat(result.operation().amount()).isEqualByComparingTo("250.75");
        assertThat(result.operation().comment()).isEqualTo("На отпуск");
        assertThat(result.fromAccount().balance()).isEqualByComparingTo("749.25");
    }

    @Test
    @DisplayName("Сумма больше остатка: INSUFFICIENT_FUNDS, операция не сохраняется")
    void insufficientFunds() {
        givenLocked(from, to);

        assertThatThrownBy(() -> service.transfer(OWNER_ID, request(1, 2, "1000.01", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.getCode()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
                    assertThat(api.getDetails()).containsEntry("balance", new BigDecimal("1000"));
                });
        assertThat(from.getBalanceKop()).isEqualTo(100_000);
        verify(operations, never()).save(any());
    }

    @Test
    @DisplayName("Дневной лимит: уже переведено 99 999,99 ₽, ещё 0,02 ₽: DAILY_LIMIT_EXCEEDED")
    void dailyLimitExceeded() {
        from = account(1L, "Основной", 50_000_000);
        givenLocked(from, to);
        when(operations.sumTransferredSince(1L, START_OF_DAY)).thenReturn(9_999_999L);

        assertThatThrownBy(() -> service.transfer(OWNER_ID, request(1, 2, "0.02", null)))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> {
                    ApiException api = (ApiException) e;
                    assertThat(api.getCode()).isEqualTo(ErrorCode.DAILY_LIMIT_EXCEEDED);
                    assertThat(api.getMessage()).contains("Доступно сегодня: 0.01 ₽");
                });
        verify(operations, never()).save(any());
    }

    @Test
    @DisplayName("Дневной лимит: перевод ровно до 100 000 ₽ проходит")
    void dailyLimitBoundaryPasses() {
        from = account(1L, "Основной", 50_000_000);
        givenLocked(from, to);
        when(operations.sumTransferredSince(1L, START_OF_DAY)).thenReturn(9_999_999L);
        when(operations.save(any(Operation.class))).thenAnswer(inv -> withId(inv.getArgument(0), 11L));

        service.transfer(OWNER_ID, request(1, 2, "0.01", null));

        assertThat(to.getBalanceKop()).isEqualTo(1);
    }

    @Test
    @DisplayName("Перевод на тот же счёт: SAME_ACCOUNT ещё до обращения к БД")
    void sameAccount() {
        assertThatThrownBy(() -> service.transfer(OWNER_ID, request(1, 1, "10", null)))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.SAME_ACCOUNT);
        verify(accounts, never()).findByIdForUpdate(anyLong());
    }

    @Test
    @DisplayName("Несуществующий счёт зачисления: ACCOUNT_NOT_FOUND")
    void accountNotFound() {
        when(accounts.findByIdForUpdate(1L)).thenReturn(Optional.of(from));
        when(accounts.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transfer(OWNER_ID, request(1, 999, "10", null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Счёт с id=999 не найден")
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("Счета блокируются в порядке возрастания id")
    void locksInIdOrder() {
        givenLocked(from, to);
        when(operations.sumTransferredSince(eq(2L), any())).thenReturn(0L);
        to.credit(500);
        when(operations.save(any(Operation.class))).thenAnswer(inv -> withId(inv.getArgument(0), 12L));

        service.transfer(OWNER_ID, request(2, 1, "5", null));

        var inOrder = org.mockito.Mockito.inOrder(accounts);
        inOrder.verify(accounts).findByIdForUpdate(1L);
        inOrder.verify(accounts).findByIdForUpdate(2L);
    }

    @Test
    @DisplayName("Чужой счёт списания: ACCOUNT_NOT_FOUND, деньги не двигаются")
    void foreignAccountIsNotFound() {
        Account foreign = new Account(STRANGER, "Чужой", 100_000, CLOCK.instant());
        foreign.setId(3L);
        when(accounts.findByIdForUpdate(2L)).thenReturn(Optional.of(to));
        when(accounts.findByIdForUpdate(3L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.transfer(OWNER_ID, request(3, 2, "10", null)))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
        assertThat(foreign.getBalanceKop()).isEqualTo(100_000);
        verify(operations, never()).save(any());
    }

    @Test
    @DisplayName("Счёт другого пользователя по id: ACCOUNT_NOT_FOUND")
    void getForeignAccount() {
        Account foreign = new Account(STRANGER, "Чужой", 0, CLOCK.instant());
        foreign.setId(3L);
        when(accounts.findById(3L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.getAccount(OWNER_ID, "3"))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("Открытие счёта с начальным остатком создаёт операцию OPENING")
    void createAccountWithOpening() {
        when(users.getReferenceById(OWNER_ID)).thenReturn(OWNER);
        when(accounts.save(any(Account.class))).thenAnswer(inv -> {
            Account a = inv.getArgument(0);
            a.setId(5L);
            return a;
        });
        ArgumentCaptor<Operation> op = ArgumentCaptor.forClass(Operation.class);

        var dto = service.createAccount(OWNER_ID, new CreateAccountRequest("  Отпуск ", TextNode.valueOf("1500,5")));

        verify(operations).save(op.capture());
        assertThat(dto.name()).isEqualTo("Отпуск");
        assertThat(dto.balance()).isEqualByComparingTo("1500.5");
        assertThat(op.getValue().getType()).isEqualTo(OperationType.OPENING);
        assertThat(op.getValue().getFromAccount()).isNull();
        assertThat(op.getValue().getAmountKop()).isEqualTo(150_050);
    }

    @Test
    @DisplayName("Открытие счёта без остатка не создаёт операцию")
    void createAccountWithoutOpening() {
        when(accounts.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        var dto = service.createAccount(OWNER_ID, new CreateAccountRequest("Пустой", null));

        assertThat(dto.balance()).isEqualByComparingTo("0");
        verify(operations, never()).save(any());
    }

    @Test
    @DisplayName("Название из пробелов: VALIDATION_ERROR")
    void blankName() {
        assertThatThrownBy(() -> service.createAccount(OWNER_ID, new CreateAccountRequest("   ", null)))
                .extracting(e -> ((ApiException) e).getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    private void givenLocked(Account a, Account b) {
        when(accounts.findByIdForUpdate(a.getId())).thenReturn(Optional.of(a));
        when(accounts.findByIdForUpdate(b.getId())).thenReturn(Optional.of(b));
    }

    private static TransferRequest request(long fromId, long toId, String amount, String comment) {
        return new TransferRequest(IntNode.valueOf((int) fromId), IntNode.valueOf((int) toId), TextNode.valueOf(amount), comment);
    }

    private static Account account(long id, String name, long balanceKop) {
        Account a = new Account(OWNER, name, balanceKop, CLOCK.instant());
        a.setId(id);
        return a;
    }

    private static User user(long id) {
        User u = new User("u" + id + "@test.ru", "hash", "Тест", Role.CLIENT, CLOCK.instant());
        u.setId(id);
        return u;
    }

    private static Operation withId(Operation op, long id) {
        op.setId(id);
        return op;
    }
}
