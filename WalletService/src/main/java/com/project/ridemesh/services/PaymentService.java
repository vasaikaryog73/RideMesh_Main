package com.project.ridemesh.services;

import com.project.ridemesh.entities.Payment;
import com.project.ridemesh.entities.enums.PaymentStatus;

public interface PaymentService {
    void processPayment(Payment payment);
    Payment createNewPayment(Long rideId, Long driverId, Long riderId, Double amount, String paymentMethod);
    void updatePaymentStatus(Payment payment, PaymentStatus paymentStatus);
}
