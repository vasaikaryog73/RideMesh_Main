package com.project.ridemesh.services.implementations;

import com.project.ridemesh.entities.WalletTransaction;
import com.project.ridemesh.repositories.WalletTransactionRepository;
import com.project.ridemesh.services.WalletTransactionService;
import org.springframework.stereotype.Service;

@Service
public class WalletTransactionServiceImpl implements WalletTransactionService {

    private final WalletTransactionRepository walletTransactionRepository;

    public WalletTransactionServiceImpl(WalletTransactionRepository walletTransactionRepository) {
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Override
    public void createNewWalletTransaction(WalletTransaction walletTransaction) {
        walletTransactionRepository.save(walletTransaction);
    }
}
