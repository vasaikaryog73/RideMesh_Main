package com.project.ridemesh.services.implementations;

import com.project.ridemesh.entities.Wallet;
import com.project.ridemesh.entities.WalletTransaction;
import com.project.ridemesh.entities.enums.TransactionMethod;
import com.project.ridemesh.entities.enums.TransactionType;
import com.project.ridemesh.exceptions.ResourceNotFoundException;
import com.project.ridemesh.repositories.WalletRepository;
import com.project.ridemesh.services.WalletService;
import com.project.ridemesh.services.WalletTransactionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionService walletTransactionService;

    public WalletServiceImpl(WalletRepository walletRepository, WalletTransactionService walletTransactionService) {
        this.walletRepository = walletRepository;
        this.walletTransactionService = walletTransactionService;
    }

    @Override
    @Transactional
    public Wallet addMoneyToWallet(Long userId, Double amount, String transactionId, Long rideId, TransactionMethod transactionMethod) {
        Wallet wallet;
        try {
            wallet = findByUserId(userId);
        } catch (ResourceNotFoundException e) {
            wallet = createNewWallet(userId);
        }
        Double prevAmount = wallet.getBalance();
        Double newAmount = prevAmount + amount;
        wallet.setBalance(newAmount);

        WalletTransaction walletTransaction = new WalletTransaction
                .WalletTransactionBuilder()
                .transactionId(transactionId)
                .transactionMethod(transactionMethod)
                .transactionType(TransactionType.CREDIT)
                .rideId(rideId)
                .amount(amount)
                .wallet(wallet)
                .build();
        walletTransactionService.createNewWalletTransaction(walletTransaction);

        return walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public Wallet subtractMoneyFromWallet(Long userId, Double amount, String transactionId, Long rideId, TransactionMethod transactionMethod) {
        Wallet wallet;
        try {
            wallet = findByUserId(userId);
        } catch (ResourceNotFoundException e) {
            wallet = createNewWallet(userId);
        }
        Double prevAmount = wallet.getBalance();
        Double newAmount = prevAmount - amount;
        wallet.setBalance(newAmount);

        WalletTransaction walletTransaction = new WalletTransaction
                .WalletTransactionBuilder()
                .transactionId(transactionId)
                .transactionMethod(transactionMethod)
                .transactionType(TransactionType.DEBIT)
                .rideId(rideId)
                .amount(amount)
                .wallet(wallet)
                .build();

        walletTransactionService.createNewWalletTransaction(walletTransaction);
        return walletRepository.save(wallet);
    }

    @Override
    public Wallet findWalletById(Long walletId) {
        return walletRepository
                .findById(walletId)
                .orElseThrow(()-> new ResourceNotFoundException("No wallet was found with ID: "+walletId));
    }

    @Override
    public Wallet findByUserId(Long userId) {
        return walletRepository
                .findByUserId(userId)
                .orElseThrow(()-> new ResourceNotFoundException("No wallet was found for user ID: "+userId));
    }

    @Override
    @Transactional
    public Wallet createNewWallet(Long userId) {
        // Check-then-act pattern for idempotency
        return walletRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUserId(userId);
                    wallet.setBalance(0.0);
                    try {
                        return walletRepository.save(wallet);
                    } catch (DataIntegrityViolationException ex) {
                        // Concurrent insert caught by unique index
                        return walletRepository.findByUserId(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found after concurrent insert."));
                    }
                });
    }
}
