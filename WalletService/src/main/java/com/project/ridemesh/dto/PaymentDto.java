package com.project.ridemesh.dto;

import com.project.ridemesh.entities.enums.PaymentMethod;
import com.project.ridemesh.entities.enums.PaymentStatus;
import java.time.LocalDateTime;

public class PaymentDto {
    private Long id;
    private PaymentMethod paymentMethod;
    private Long rideId;
    private Long driverId;
    private Long riderId;
    private PaymentStatus paymentStatus;
    private Double amount;
    private LocalDateTime paymentTime;

    public PaymentDto() {
    }

    public PaymentDto(Long id, PaymentMethod paymentMethod, Long rideId, Long driverId, Long riderId, PaymentStatus paymentStatus, Double amount, LocalDateTime paymentTime) {
        this.id = id;
        this.paymentMethod = paymentMethod;
        this.rideId = rideId;
        this.driverId = driverId;
        this.riderId = riderId;
        this.paymentStatus = paymentStatus;
        this.amount = amount;
        this.paymentTime = paymentTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Long getRideId() {
        return rideId;
    }

    public void setRideId(Long rideId) {
        this.rideId = rideId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Long getRiderId() {
        return riderId;
    }

    public void setRiderId(Long riderId) {
        this.riderId = riderId;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(LocalDateTime paymentTime) {
        this.paymentTime = paymentTime;
    }
}
