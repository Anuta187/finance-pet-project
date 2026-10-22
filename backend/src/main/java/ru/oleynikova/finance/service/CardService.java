package ru.oleynikova.finance.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.oleynikova.finance.dto.CardDto;
import ru.oleynikova.finance.dto.IssueCardRequest;
import ru.oleynikova.finance.entity.Account;
import ru.oleynikova.finance.entity.Card;
import ru.oleynikova.finance.entity.CardStatus;
import ru.oleynikova.finance.error.ApiException;
import ru.oleynikova.finance.repository.AccountRepository;
import ru.oleynikova.finance.repository.CardRepository;
import ru.oleynikova.finance.support.Ids;
import ru.oleynikova.finance.support.Translit;

@Service
public class CardService {

    static final String BIN = "2200";
    static final String PAYMENT_SYSTEM = "МИР";
    static final int VALIDITY_YEARS = 4;

    private final CardRepository cards;
    private final AccountRepository accounts;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public CardService(CardRepository cards, AccountRepository accounts, Clock clock) {
        this.cards = cards;
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CardDto> list(long userId) {
        return cards.findAllByOwner(userId).stream().map(CardDto::of).toList();
    }

    @Transactional
    public CardDto issue(long userId, IssueCardRequest request) {
        long accountId = Ids.parse(request == null ? null : request.accountId(), "accountId");
        Account account = accounts.findById(accountId).filter(a -> a.isOwnedBy(userId))
                .orElseThrow(() -> ApiException.accountNotFound(accountId));
        String holder = request.holderName() == null || request.holderName().isBlank()
                ? Translit.cardHolder(account.getOwner().getFullName())
                : request.holderName().trim().toUpperCase(Locale.ROOT);
        if (holder.length() > 100) {
            throw ApiException.validation("holderName", "Имя держателя — не более 100 символов");
        }
        return CardDto.of(cards.save(newCard(account, holder)));
    }

    public Card newCard(Account account, String holder) {
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS);
        String number = generateNumber();
        return new Card(account, number.substring(0, 4), number.substring(12), PAYMENT_SYSTEM, holder,
                YearMonth.now(clock).plusYears(VALIDITY_YEARS), now);
    }

    @Transactional
    public CardDto setStatus(long userId, String rawId, CardStatus status) {
        Card card = findOwn(userId, Ids.parse(rawId, "id"));
        card.setStatus(status);
        return CardDto.of(card);
    }

    public Card findOwn(long userId, long cardId) {
        return cards.findById(cardId).filter(c -> c.getAccount().isOwnedBy(userId))
                .orElseThrow(() -> ApiException.cardNotFound(cardId));
    }

    // Номер с контрольной цифрой по алгоритму Луна
    String generateNumber() {
        StringBuilder sb = new StringBuilder(BIN);
        while (sb.length() < 15) {
            sb.append(random.nextInt(10));
        }
        return sb.append(luhnCheckDigit(sb.toString())).toString();
    }

    static int luhnCheckDigit(String first15) {
        int sum = 0;
        for (int i = 0; i < first15.length(); i++) {
            int d = first15.charAt(first15.length() - 1 - i) - '0';
            if (i % 2 == 0) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
        }
        return (10 - sum % 10) % 10;
    }
}
