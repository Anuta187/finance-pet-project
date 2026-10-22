package ru.oleynikova.finance.entity;

import java.time.Instant;
import java.time.LocalDate;

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
@Table(name = "autopayments")
public class Autopayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id")
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "amount_kop", nullable = false)
    private long amountKop;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 16)
    private AutopaymentPeriod period;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "next_run_date", nullable = false)
    private LocalDate nextRunDate;

    @Column(name = "run_count", nullable = false)
    private int runCount;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "last_status", length = 16)
    private AutopaymentStatus lastStatus;

    @Column(name = "last_error", length = 200)
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Autopayment() {
    }

    public Autopayment(Account account, Card card, Category category, String name, long amountKop,
                       AutopaymentPeriod period, LocalDate startDate, Instant createdAt) {
        this.account = account;
        this.card = card;
        this.category = category;
        this.name = name;
        this.amountKop = amountKop;
        this.period = period;
        this.startDate = startDate;
        this.nextRunDate = startDate;
        this.createdAt = createdAt;
    }

    // Пропущенные периоды не догоняем, следующая дата всегда позже today
    public void markProcessed(LocalDate today, Instant at, AutopaymentStatus status, String error) {
        do {
            runCount++;
            nextRunDate = period.nth(startDate, runCount);
        } while (!nextRunDate.isAfter(today));
        lastRunAt = at;
        lastStatus = status;
        lastError = error;
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

    public Card getCard() {
        return card;
    }

    public Category getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public long getAmountKop() {
        return amountKop;
    }

    public AutopaymentPeriod getPeriod() {
        return period;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getNextRunDate() {
        return nextRunDate;
    }

    public int getRunCount() {
        return runCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public AutopaymentStatus getLastStatus() {
        return lastStatus;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
