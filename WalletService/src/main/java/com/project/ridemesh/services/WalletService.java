package com.project.ridemesh.services;

import com.project.ridemesh.entities.Wallet;
import com.project.ridemesh.entities.enums.TransactionMethod;

public interface WalletService {
    Wallet addMoneyToWallet(Long userId, Double amount, String transactionId, Long rideId, TransactionMethod transactionMethod);
    Wallet subtractMoneyFromWallet(Long userId, Double amount, String transactionId, Long rideId, TransactionMethod transactionMethod);
    Wallet findWalletById(Long walletId);
    Wallet findByUserId(Long userId);
    Wallet createNewWallet(Long userId);
}
