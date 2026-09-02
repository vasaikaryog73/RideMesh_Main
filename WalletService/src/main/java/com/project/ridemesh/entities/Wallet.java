package com.project.ridemesh.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

@Entity
@Table(indexes = {
        @Index(name = "idx_wallet_user", columnList = "userId", unique = true)
})
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "User ID is required")
    @Column(unique = true, nullable = false)
    private Long userId;

    @NotNull(message = "Balance cannot be null")
    @PositiveOrZero(message = "Balance must be zero or a positive value")
    private Double balance = 0.0;

    @OneToMany(mappedBy = "wallet", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<WalletTransaction> transactions;

    public Wallet() {
    }

    public Wallet(Long id, Long userId, Double balance, List<WalletTransaction> transactions) {
        this.id = id;
        this.userId = userId;
        this.balance = balance;
        this.transactions = transactions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public List<WalletTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<WalletTransaction> transactions) {
        this.transactions = transactions;
    }
}
