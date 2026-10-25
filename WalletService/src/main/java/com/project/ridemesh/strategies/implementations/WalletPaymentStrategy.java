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
        double paymentAmount = payment.getAmount();

        // 1. Credit driver (fare)
        walletService.addMoneyToWallet(payment.getDriverId(),
                paymentAmount,
                null,
                payment.getRideId(),
                TransactionMethod.RIDE);

        // 2. Debit rider (fare)
        walletService.subtractMoneyFromWallet(payment.getRiderId(),
                paymentAmount,
                null,
                payment.getRideId(),
                TransactionMethod.RIDE);

        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
    }
}
