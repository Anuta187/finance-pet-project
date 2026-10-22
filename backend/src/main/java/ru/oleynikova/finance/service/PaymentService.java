package ru.oleynikova.finance.service;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.oleynikova.finance.dto.CategoryDto;
import ru.oleynikova.finance.dto.PaymentRequest;
import ru.oleynikova.finance.dto.PaymentResult;
import ru.oleynikova.finance.entity.Account;
import ru.oleynikova.finance.entity.Autopayment;
import ru.oleynikova.finance.entity.Card;
import ru.oleynikova.finance.entity.CardStatus;
import ru.oleynikova.finance.entity.Category;
import ru.oleynikova.finance.entity.Operation;
import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.error.ErrorCode;
import ru.oleynikova.finance.repository.AccountRepository;
import ru.oleynikova.finance.repository.CategoryRepository;
import ru.oleynikova.finance.repository.OperationRepository;
import ru.oleynikova.finance.support.Ids;
import ru.oleynikova.finance.support.Money;

/** Оплата услуг со счёта или картой. Используется и автоплатежами. */
@Service
public class PaymentService {

    private final AccountRepository accounts;
    private final OperationRepository operations;
    private final CategoryRepository categories;
    private final CardService cardService;
    private final Clock clock;

    public PaymentService(AccountRepository accounts, OperationRepository operations, CategoryRepository categories,
                          CardService cardService, Clock clock) {
        this.accounts = accounts;
        this.operations = operations;
        this.categories = categories;
        this.cardService = cardService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> categories() {
        return categories.findAllByOrderBySortOrderAsc().stream().map(CategoryDto::of).toList();
    }

    @Transactional
    public PaymentResult pay(long userId, PaymentRequest request) {
        if (request == null) {
            throw ApiException.validation("categoryId", "Поле «categoryId» обязательно");
        }
        long categoryId = Ids.parse(request.categoryId(), "categoryId");
        Source source = Source.of(request.accountId(), request.cardId());
        long amountKop = Money.parseAmount(request.amount(), "amount", false);
        String comment = FinanceService.normalizeComment(request.comment());
        Category category = findCategory(categoryId);

        Operation op = execute(userId, source, category, amountKop, comment, null);
        return new PaymentResult(Mappers.operation(op), Mappers.account(op.getFromAccount()));
    }

    public Category findCategory(long id) {
        return categories.findById(id).orElseThrow(() -> ApiException.categoryNotFound(id));
    }

    // Без @Transactional: вызывается внутри уже открытой транзакции
    public Operation execute(long userId, Source source, Category category, long amountKop, String comment,
                             Autopayment autopayment) {
        Card card = null;
        long accountId;
        if (source.cardId() != null) {
            card = cardService.findOwn(userId, source.cardId());
            ensureUsable(card);
            accountId = card.getAccount().getId();
        } else {
            accountId = source.accountId();
        }
        Account account = accounts.findByIdForUpdate(accountId).filter(a -> a.isOwnedBy(userId))
                .orElseThrow(() -> ApiException.accountNotFound(accountId));
        if (account.getBalanceKop() < amountKop) {
            throw FinanceService.insufficientFunds(account, amountKop);
        }
        account.debit(amountKop);
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
        return operations.save(Operation.payment(account, amountKop, comment, now, category, card, autopayment));
    }

    public void ensureUsable(Card card) {
        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new ApiException(ErrorCode.CARD_BLOCKED, "Карта •••• " + card.getLast4() + " заблокирована");
        }
        if (card.isExpiredAt(YearMonth.now(clock))) {
            throw new ApiException(ErrorCode.CARD_EXPIRED, "Срок действия карты •••• " + card.getLast4() + " истёк");
        }
    }

    public record Source(Long accountId, Long cardId) {

        public static Source of(com.fasterxml.jackson.databind.JsonNode rawAccountId,
                                com.fasterxml.jackson.databind.JsonNode rawCardId) {
            Long accountId = Ids.parseOptional(rawAccountId, "accountId");
            Long cardId = Ids.parseOptional(rawCardId, "cardId");
            if ((accountId == null) == (cardId == null)) {
                throw ApiException.validation("source", "Укажите ровно один источник списания: accountId или cardId");
            }
            return new Source(accountId, cardId);
        }
    }
}
