package com.project.ridemesh.strategies.implementations;

import com.project.ridemesh.entities.Payment;
import com.project.ridemesh.entities.enums.PaymentStatus;
import com.project.ridemesh.entities.enums.TransactionMethod;
import com.project.ridemesh.services.WalletService;
import com.project.ridemesh.strategies.PaymentStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletPaymentStrategy implements PaymentStrategy {

    private final WalletService walletService;

    public WalletPaymentStrategy(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    @Transactional
    public void processPayment(Payment payment) {
        double paymentAddedToWallet = payment.getAmount() - (payment.getAmount() * PLATFORM_FEE);

        // 1. Credit driver (fare minus platform fee)
        walletService.addMoneyToWallet(payment.getDriverId(),
                paymentAddedToWallet,
                null,
                payment.getRideId(),
                TransactionMethod.RIDE);

        // 2. Debit rider (full fare)
        walletService.subtractMoneyFromWallet(payment.getRiderId(),
                payment.getAmount(),
                null,
                payment.getRideId(),
                TransactionMethod.RIDE);

        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
    }
}
