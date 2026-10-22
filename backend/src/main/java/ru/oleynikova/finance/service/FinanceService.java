package ru.oleynikova.finance.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.oleynikova.finance.dto.AccountDetailsDto;
import ru.oleynikova.finance.dto.AccountDto;
import ru.oleynikova.finance.dto.CreateAccountRequest;
import ru.oleynikova.finance.dto.OperationDto;
import ru.oleynikova.finance.dto.TransferRequest;
import ru.oleynikova.finance.dto.TransferResult;
import ru.oleynikova.finance.entity.Account;
import ru.oleynikova.finance.entity.Operation;
import ru.oleynikova.finance.entity.OperationType;
import ru.oleynikova.finance.entity.User;
import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.error.ErrorCode;
import ru.oleynikova.finance.repository.AccountRepository;
import ru.oleynikova.finance.repository.OperationRepository;
import ru.oleynikova.finance.repository.UserRepository;
import ru.oleynikova.finance.support.Ids;
import ru.oleynikova.finance.support.Money;

/** Счета, переводы и история. Чужие счета для пользователя считаются несуществующими. */
@Service
public class FinanceService {

    public static final int MAX_NAME_LENGTH = 50;
    public static final int MAX_COMMENT_LENGTH = 200;
    public static final int DEFAULT_LIMIT = 100;
    public static final int MAX_LIMIT = 500;

    private final AccountRepository accounts;
    private final OperationRepository operations;
    private final UserRepository users;
    private final Clock clock;
    private final long dailyLimitKop;

    public FinanceService(AccountRepository accounts, OperationRepository operations, UserRepository users, Clock clock,
                          @Value("${app.daily-limit-rub:100000}") BigDecimal dailyLimitRub) {
        this.accounts = accounts;
        this.operations = operations;
        this.users = users;
        this.clock = clock;
        this.dailyLimitKop = dailyLimitRub.movePointRight(2).longValueExact();
    }

    @Transactional(readOnly = true)
    public List<AccountDto> listAccounts(long userId) {
        return accounts.findAllByOwnerIdOrderByIdAsc(userId).stream().map(Mappers::account).toList();
    }

    @Transactional(readOnly = true)
    public AccountDetailsDto getAccount(long userId, String rawId) {
        long id = Ids.parse(rawId, "id");
        Account a = accounts.findById(id).filter(acc -> acc.isOwnedBy(userId))
                .orElseThrow(() -> ApiException.accountNotFound(id));
        long used = transferredToday(id);
        return new AccountDetailsDto(a.getId(), a.getName(), a.getCurrency(), Money.toRub(a.getBalanceKop()),
                a.getCreatedAt(), Money.toRub(dailyLimitKop), Money.toRub(used),
                Money.toRub(Math.max(dailyLimitKop - used, 0)));
    }

    @Transactional
    public AccountDto createAccount(long userId, CreateAccountRequest request) {
        return Mappers.account(openAccount(users.getReferenceById(userId), request));
    }

    @Transactional
    public Account openAccount(User owner, CreateAccountRequest request) {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw ApiException.validation("name", "Название счёта обязательно");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw ApiException.validation("name", "Название счёта — не более 50 символов");
        }
        var rawBalance = request.initialBalance();
        long initialKop = rawBalance == null || rawBalance.isNull()
                || (rawBalance.isTextual() && rawBalance.asText().isEmpty())
                ? 0
                : Money.parseAmount(rawBalance, "initialBalance", true);

        Instant now = now();
        Account account = accounts.save(new Account(owner, name, initialKop, now));
        if (initialKop > 0) {
            operations.save(new Operation(OperationType.OPENING, null, account, initialKop, "Начальный остаток", now));
        }
        return account;
    }

    @Transactional
    public TransferResult transfer(long userId, TransferRequest request) {
        if (request == null) {
            throw ApiException.validation("fromAccountId", "Поле «fromAccountId» обязательно");
        }
        long fromId = Ids.parse(request.fromAccountId(), "fromAccountId");
        long toId = Ids.parse(request.toAccountId(), "toAccountId");
        long amountKop = Money.parseAmount(request.amount(), "amount", false);
        String comment = normalizeComment(request.comment());

        if (fromId == toId) {
            throw new ApiException(ErrorCode.SAME_ACCOUNT, "Нельзя перевести деньги на тот же счёт");
        }

        // Блокируем счета по возрастанию id, чтобы встречные переводы не попали в дедлок
        var firstLocked = accounts.findByIdForUpdate(Math.min(fromId, toId));
        var secondLocked = accounts.findByIdForUpdate(Math.max(fromId, toId));
        Account from = (fromId < toId ? firstLocked : secondLocked).filter(a -> a.isOwnedBy(userId))
                .orElseThrow(() -> ApiException.accountNotFound(fromId));
        Account to = (fromId < toId ? secondLocked : firstLocked).filter(a -> a.isOwnedBy(userId))
                .orElseThrow(() -> ApiException.accountNotFound(toId));

        if (from.getBalanceKop() < amountKop) {
            throw insufficientFunds(from, amountKop);
        }

        long used = transferredToday(fromId);
        if (used + amountKop > dailyLimitKop) {
            BigDecimal remaining = Money.toRub(Math.max(dailyLimitKop - used, 0));
            throw new ApiException(ErrorCode.DAILY_LIMIT_EXCEEDED,
                    "Превышен дневной лимит переводов (" + Money.toRub(dailyLimitKop).toPlainString()
                            + " ₽). Доступно сегодня: " + remaining.toPlainString() + " ₽",
                    Map.of("dailyLimit", Money.toRub(dailyLimitKop), "dailyLimitRemaining", remaining));
        }

        from.debit(amountKop);
        to.credit(amountKop);
        Operation op = operations.save(new Operation(OperationType.TRANSFER, from, to, amountKop, comment, now()));
        return new TransferResult(Mappers.operation(op), Mappers.account(from), Mappers.account(to));
    }

    @Transactional(readOnly = true)
    public List<OperationDto> listOperations(long userId, String rawAccountId, String rawLimit) {
        Long accountId = null;
        if (rawAccountId != null && !rawAccountId.isEmpty()) {
            long id = Ids.parse(rawAccountId, "accountId");
            if (!accounts.existsByIdAndOwnerId(id, userId)) {
                throw ApiException.accountNotFound(id);
            }
            accountId = id;
        }
        var page = PageRequest.of(0, parseLimit(rawLimit));
        List<Operation> list = accountId == null
                ? operations.findLatestByOwner(userId, page)
                : operations.findLatestByAccount(accountId, page);
        return list.stream().map(Mappers::operation).toList();
    }

    static ApiException insufficientFunds(Account from, long amountKop) {
        return new ApiException(ErrorCode.INSUFFICIENT_FUNDS, "Недостаточно средств на счёте списания",
                Map.of("balance", Money.toRub(from.getBalanceKop()), "amount", Money.toRub(amountKop)));
    }

    private long transferredToday(long accountId) {
        Instant startOfDay = LocalDate.now(clock).atStartOfDay(clock.getZone()).toInstant();
        return operations.sumTransferredSince(accountId, startOfDay);
    }

    private Instant now() {
        return Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }

    static int parseLimit(String raw) {
        if (raw == null) {
            return DEFAULT_LIMIT;
        }
        try {
            int limit = Integer.parseInt(raw.trim());
            if (limit >= 1 && limit <= MAX_LIMIT) {
                return limit;
            }
        } catch (NumberFormatException ignored) {
        }
        throw ApiException.validation("limit", "Параметр «limit» должен быть целым числом от 1 до 500");
    }

    static String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        String trimmed = comment.trim();
        return trimmed.length() > MAX_COMMENT_LENGTH ? trimmed.substring(0, MAX_COMMENT_LENGTH) : trimmed;
    }
}
