package ru.oleynikova.finance.dto;

import java.time.Instant;

import ru.oleynikova.finance.entity.Card;
import ru.oleynikova.finance.entity.CardStatus;

public record CardDto(Long id, Long accountId, String accountName, String maskedNumber, String last4,
                      String paymentSystem, String holderName, int expiryMonth, int expiryYear, String expiry,
                      CardStatus status, Instant createdAt) {

    public static CardDto of(Card c) {
        return new CardDto(c.getId(), c.getAccount().getId(), c.getAccount().getName(), c.maskedNumber(), c.getLast4(),
                c.getPaymentSystem(), c.getHolderName(), c.getExpiryMonth(), c.getExpiryYear(),
                String.format("%02d/%02d", c.getExpiryMonth(), c.getExpiryYear() % 100), c.getStatus(), c.getCreatedAt());
    }
}
