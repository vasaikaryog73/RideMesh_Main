package com.project.ridemesh.strategies.managers;

import com.project.ridemesh.entities.enums.PaymentMethod;
import com.project.ridemesh.strategies.PaymentStrategy;
import com.project.ridemesh.strategies.implementations.CashPaymentStrategy;
import com.project.ridemesh.strategies.implementations.RazorpayPaymentStrategy;
import com.project.ridemesh.strategies.implementations.WalletPaymentStrategy;
import org.springframework.stereotype.Component;

@Component
public class PaymentStrategyManager {

    private final CashPaymentStrategy cashPaymentStrategy;
    private final WalletPaymentStrategy walletPaymentStrategy;
    private final RazorpayPaymentStrategy razorpayPaymentStrategy;

    public PaymentStrategyManager(CashPaymentStrategy cashPaymentStrategy, WalletPaymentStrategy walletPaymentStrategy, RazorpayPaymentStrategy razorpayPaymentStrategy) {
        this.cashPaymentStrategy = cashPaymentStrategy;
        this.walletPaymentStrategy = walletPaymentStrategy;
        this.razorpayPaymentStrategy = razorpayPaymentStrategy;
    }

    public PaymentStrategy paymentStrategy(PaymentMethod paymentMethod) {
        switch (paymentMethod) {
            case CASH:
                return cashPaymentStrategy;
            case WALLET:
                return walletPaymentStrategy;
            case RAZORPAY:
            case UPI:
                return razorpayPaymentStrategy;
            default:
                throw new IllegalArgumentException("Unknown payment method: " + paymentMethod);
        }
    }
}
