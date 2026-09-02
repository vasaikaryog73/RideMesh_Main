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
        double paymentCommission = payment.getAmount() * PLATFORM_FEE;

        walletService.subtractMoneyFromWallet(payment.getDriverId(),
                paymentCommission,
                null, // Cash payments do not have external transaction IDs
                payment.getRideId(),
                TransactionMethod.RIDE);

        payment.setPaymentStatus(PaymentStatus.CONFIRMED);
    }
}
