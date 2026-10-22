package ru.oleynikova.finance.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.oleynikova.finance.dto.AutopaymentDto;
import ru.oleynikova.finance.dto.AutopaymentRunResult;
import ru.oleynikova.finance.dto.CreateAutopaymentRequest;
import ru.oleynikova.finance.entity.Account;
import ru.oleynikova.finance.entity.Autopayment;
import ru.oleynikova.finance.entity.AutopaymentPeriod;
import ru.oleynikova.finance.entity.AutopaymentStatus;
import ru.oleynikova.finance.entity.Card;
import ru.oleynikova.finance.entity.Category;
import ru.oleynikova.finance.entity.Operation;
import ru.oleynikova.finance.entity.User;
import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.error.ErrorCode;
import ru.oleynikova.finance.repository.AccountRepository;
import ru.oleynikova.finance.repository.AutopaymentRepository;
import ru.oleynikova.finance.support.Ids;
import ru.oleynikova.finance.support.Money;

@Service
public class AutopaymentService {

    private static final Logger log = LoggerFactory.getLogger(AutopaymentService.class);
    static final int MAX_NAME_LENGTH = 100;

    private final AutopaymentRepository autopayments;
    private final AccountRepository accounts;
    private final PaymentService payments;
    private final CardService cards;
    private final TransactionTemplate tx;
    private final Clock clock;

    public AutopaymentService(AutopaymentRepository autopayments, AccountRepository accounts, PaymentService payments,
                              CardService cards, PlatformTransactionManager txManager, Clock clock) {
        this.autopayments = autopayments;
        this.accounts = accounts;
        this.payments = payments;
        this.cards = cards;
        this.tx = new TransactionTemplate(txManager);
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AutopaymentDto> list(long userId) {
        return autopayments.findAllByOwner(userId).stream().map(Mappers::autopayment).toList();
    }

    @Transactional
    public AutopaymentDto create(long userId, CreateAutopaymentRequest request) {
        if (request == null) {
            throw ApiException.validation("name", "Название автоплатежа обязательно");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw ApiException.validation("name", "Название автоплатежа обязательно");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw ApiException.validation("name", "Название автоплатежа — не более 100 символов");
        }
        long categoryId = Ids.parse(request.categoryId(), "categoryId");
        PaymentService.Source source = PaymentService.Source.of(request.accountId(), request.cardId());
        long amountKop = Money.parseAmount(request.amount(), "amount", false);
        AutopaymentPeriod period = parsePeriod(request.period());
        LocalDate today = LocalDate.now(clock);
        LocalDate start = parseStartDate(request.startDate(), today);

        Category category = payments.findCategory(categoryId);
        Card card = null;
        Account account;
        if (source.cardId() != null) {
            card = cards.findOwn(userId, source.cardId());
            payments.ensureUsable(card);
            account = card.getAccount();
        } else {
            account = accounts.findById(source.accountId()).filter(a -> a.isOwnedBy(userId))
                    .orElseThrow(() -> ApiException.accountNotFound(source.accountId()));
        }
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
        Autopayment ap = autopayments.save(new Autopayment(account, card, category, name, amountKop, period, start, now));
        return Mappers.autopayment(ap);
    }

    @Transactional
    public AutopaymentDto disable(long userId, String rawId) {
        long id = Ids.parse(rawId, "id");
        Autopayment ap = autopayments.findById(id).filter(a -> a.getAccount().isOwnedBy(userId))
                .orElseThrow(() -> ApiException.autopaymentNotFound(id));
        ap.setActive(false);
        return Mappers.autopayment(ap);
    }

    // Каждый автоплатёж в своей транзакции, чтобы ошибка одного не откатывала остальные
    public AutopaymentRunResult runDue() {
        LocalDate today = LocalDate.now(clock);
        List<AutopaymentRunResult.Item> results = new ArrayList<>();
        for (Long id : autopayments.findDueIds(today)) {
            AutopaymentRunResult.Item item = tx.execute(status -> {
                Autopayment ap = autopayments.findById(id).orElse(null);
                if (ap == null || !ap.isActive() || ap.getNextRunDate().isAfter(today)) {
                    return null;   // уже обработан параллельным запуском
                }
                return process(ap, today);
            });
            if (item != null) {
                results.add(item);
            }
        }
        int ok = (int) results.stream().filter(r -> r.status() == AutopaymentStatus.SUCCESS).count();
        if (!results.isEmpty()) {
            log.info("Автоплатежи за {}: обработано {}, успешно {}, с ошибкой {}", today, results.size(), ok,
                    results.size() - ok);
        }
        return new AutopaymentRunResult(today, results.size(), ok, results.size() - ok, results);
    }

    AutopaymentRunResult.Item process(Autopayment ap, LocalDate today) {
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
        User owner = ap.getAccount().getOwner();
        try {
            if (owner == null || owner.isBlocked()) {
                throw ApiException.userBlocked();
            }
            PaymentService.Source source = ap.getCard() != null
                    ? new PaymentService.Source(null, ap.getCard().getId())
                    : new PaymentService.Source(ap.getAccount().getId(), null);
            Operation op = payments.execute(owner.getId(), source, ap.getCategory(), ap.getAmountKop(),
                    "Автоплатёж: " + ap.getName(), ap);
            ap.markProcessed(today, now, AutopaymentStatus.SUCCESS, null);
            return new AutopaymentRunResult.Item(ap.getId(), AutopaymentStatus.SUCCESS, null, op.getId());
        } catch (ApiException e) {
            String error = e.getCode().name() + ": " + e.getMessage();
            ap.markProcessed(today, now, AutopaymentStatus.FAILED, error.length() > 200 ? error.substring(0, 200) : error);
            return new AutopaymentRunResult.Item(ap.getId(), AutopaymentStatus.FAILED, e.getCode().name(), null);
        }
    }

    static AutopaymentPeriod parsePeriod(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.validation("period", "Поле «period» обязательно");
        }
        try {
            return AutopaymentPeriod.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.validation("period", "Поле «period» должно быть одним из: DAILY, WEEKLY, MONTHLY");
        }
    }

    static LocalDate parseStartDate(String raw, LocalDate today) {
        if (raw == null || raw.isBlank()) {
            return today;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.validation("startDate", "Дата начала должна быть в формате ГГГГ-ММ-ДД");
        }
        if (date.isBefore(today)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Дата начала не может быть в прошлом",
                    java.util.Map.of("field", "startDate", "today", today.toString()));
        }
        return date;
    }
}
