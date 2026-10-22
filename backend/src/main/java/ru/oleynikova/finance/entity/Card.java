package ru.oleynikova.finance.entity;

import java.time.Instant;
import java.time.YearMonth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "bin4", nullable = false, length = 4)
    private String bin4;

    @Column(nullable = false, length = 4)
    private String last4;

    @Column(name = "payment_system", nullable = false, length = 16)
    private String paymentSystem;

    @Column(name = "holder_name", nullable = false, length = 100)
    private String holderName;

    @Column(name = "expiry_month", nullable = false)
    private int expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private int expiryYear;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Card() {
    }

    public Card(Account account, String bin4, String last4, String paymentSystem, String holderName,
                YearMonth expiry, Instant createdAt) {
        this.account = account;
        this.bin4 = bin4;
        this.last4 = last4;
        this.paymentSystem = paymentSystem;
        this.holderName = holderName;
        this.expiryMonth = expiry.getMonthValue();
        this.expiryYear = expiry.getYear();
        this.createdAt = createdAt;
    }

    // Полный номер карты не хранится
    public String maskedNumber() {
        return bin4 + " **** **** " + last4;
    }

    public boolean isExpiredAt(YearMonth current) {
        return current.isAfter(YearMonth.of(expiryYear, expiryMonth));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public String getLast4() {
        return last4;
    }

    public String getPaymentSystem() {
        return paymentSystem;
    }

    public String getHolderName() {
        return holderName;
    }

    public int getExpiryMonth() {
        return expiryMonth;
    }

    public int getExpiryYear() {
        return expiryYear;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
