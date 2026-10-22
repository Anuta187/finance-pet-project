package ru.oleynikova.finance.entity;

import java.time.Instant;

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
@Table(name = "operations")
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private OperationType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    // У платежа нет счёта зачисления
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @Column(name = "amount_kop", nullable = false)
    private long amountKop;

    @Column(length = 200)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autopayment_id")
    private Autopayment autopayment;

    protected Operation() {
    }

    public Operation(OperationType type, Account fromAccount, Account toAccount, long amountKop, String comment,
                     Instant createdAt) {
        this.type = type;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amountKop = amountKop;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public static Operation payment(Account from, long amountKop, String comment, Instant createdAt,
                                    Category category, Card card, Autopayment autopayment) {
        Operation op = new Operation(OperationType.PAYMENT, from, null, amountKop, comment, createdAt);
        op.category = category;
        op.card = card;
        op.autopayment = autopayment;
        return op;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OperationType getType() {
        return type;
    }

    public Account getFromAccount() {
        return fromAccount;
    }

    public Account getToAccount() {
        return toAccount;
    }

    public long getAmountKop() {
        return amountKop;
    }

    public String getComment() {
        return comment;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Category getCategory() {
        return category;
    }

    public Card getCard() {
        return card;
    }

    public Autopayment getAutopayment() {
        return autopayment;
    }
}
