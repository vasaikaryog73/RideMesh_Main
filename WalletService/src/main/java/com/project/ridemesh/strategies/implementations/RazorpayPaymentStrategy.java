package com.project.ridemesh.strategies.implementations;

import com.project.ridemesh.entities.Payment;
import com.project.ridemesh.entities.enums.PaymentStatus;
import com.project.ridemesh.strategies.PaymentStrategy;
import org.springframework.stereotype.Service;

@Service
public class RazorpayPaymentStrategy implements PaymentStrategy {

    @Override
    public void processPayment(Payment payment) {
        // Set state to PENDING and wait for Razorpay Webhook notification to flip to CONFIRMED
        payment.setPaymentStatus(PaymentStatus.PENDING);
    }
}
