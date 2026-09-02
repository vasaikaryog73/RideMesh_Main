package com.project.ridemesh.strategies;

import com.project.ridemesh.entities.Payment;

public interface PaymentStrategy {
    Double PLATFORM_FEE = 0.3;
    void processPayment(Payment payment);
}
