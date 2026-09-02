package com.project.ridemesh.dto;

public class WalletDto {
    private Long id;
    private Double balance;

    public WalletDto() {
    }

    public WalletDto(Long id, Double balance) {
        this.id = id;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }
}
