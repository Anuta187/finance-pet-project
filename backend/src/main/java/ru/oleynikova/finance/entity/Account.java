package ru.oleynikova.finance.entity;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User owner;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 3)
    private String currency = "RUB";

    @Column(name = "balance_kop", nullable = false)
    private long balanceKop;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Account() {
    }

    public Account(User owner, String name, long balanceKop, Instant createdAt) {
        this.owner = owner;
        this.name = name;
        this.balanceKop = balanceKop;
        this.createdAt = createdAt;
    }

    public boolean isOwnedBy(long userId) {
        return owner != null && Objects.equals(owner.getId(), userId);
    }

    public void debit(long kop) {
        this.balanceKop -= kop;
    }

    public void credit(long kop) {
        this.balanceKop += kop;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public String getCurrency() {
        return currency;
    }

    public long getBalanceKop() {
        return balanceKop;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
