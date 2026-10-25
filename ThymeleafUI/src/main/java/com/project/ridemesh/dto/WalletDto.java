package com.project.ridemesh.dto;

import java.util.List;

public class WalletDto {
    private Long id;
    private UserDto user;
    private Double balance;
    private List<WalletTransactionDto> transactions;

    public WalletDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public List<WalletTransactionDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<WalletTransactionDto> transactions) {
        this.transactions = transactions;
    }

    public String getFormattedId() {
        return id != null ? String.format("%05d", id) : "00000";
    }
}
