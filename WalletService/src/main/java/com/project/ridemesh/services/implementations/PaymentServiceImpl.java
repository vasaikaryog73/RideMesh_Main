package com.project.ridemesh.services.implementations;

import com.project.ridemesh.entities.Payment;
import com.project.ridemesh.entities.enums.PaymentMethod;
import com.project.ridemesh.entities.enums.PaymentStatus;
import com.project.ridemesh.repositories.PaymentRepository;
import com.project.ridemesh.services.PaymentService;
import com.project.ridemesh.strategies.managers.PaymentStrategyManager;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStrategyManager paymentStrategyManager;

    public PaymentServiceImpl(PaymentRepository paymentRepository, PaymentStrategyManager paymentStrategyManager) {
        this.paymentRepository = paymentRepository;
        this.paymentStrategyManager = paymentStrategyManager;
    }

    @Override
    public void processPayment(Payment payment) {
        paymentStrategyManager
                .paymentStrategy(payment.getPaymentMethod())
                .processPayment(payment);

        updatePaymentStatus(payment, payment.getPaymentStatus());
    }

    @Override
    public Payment createNewPayment(Long rideId, Long driverId, Long riderId, Double amount, String paymentMethod) {
        Payment payment = new Payment.PaymentBuilder()
                .amount(amount)
                .paymentMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()))
                .paymentStatus(PaymentStatus.PENDING)
                .rideId(rideId)
                .driverId(driverId)
                .riderId(riderId)
                .build();

        return paymentRepository.save(payment);
    }

    @Override
    public void updatePaymentStatus(Payment payment, PaymentStatus paymentStatus) {
        payment.setPaymentStatus(paymentStatus);
        paymentRepository.save(payment);
    }
}
