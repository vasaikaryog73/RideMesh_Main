package com.project.ridemesh.strategies.implementations;

import com.project.ridemesh.entities.Payment;
import com.project.ridemesh.entities.enums.PaymentStatus;
import com.project.ridemesh.entities.enums.TransactionMethod;
import com.project.ridemesh.services.WalletService;
import com.project.ridemesh.strategies.PaymentStrategy;
import org.springframework.stereotype.Service;

@Service
public class CashPaymentStrategy implements PaymentStrategy {

    private final WalletService walletService;

    public CashPaymentStrategy(WalletService walletService) {
        this.walletService = walletService;
    }

    @Override
    public void processPayment(Payment payment) {
        // Cash payment confirmed directly between rider and driver; no wallet balance increase/decrease on either side
        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
    }
}
