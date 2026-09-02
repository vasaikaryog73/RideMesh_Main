package com.project.ridemesh.entities;

import com.project.ridemesh.entities.enums.PaymentMethod;
import com.project.ridemesh.entities.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_payment_ride", columnList = "rideId", unique = true),
        @Index(name = "idx_payment_rider", columnList = "riderId"),
        @Index(name = "idx_payment_driver", columnList = "driverId")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @NotNull(message = "Ride ID is required")
    @Column(unique = true, nullable = false)
    private Long rideId;

    @NotNull(message = "Driver ID is required")
    private Long driverId;

    @NotNull(message = "Rider ID is required")
    private Long riderId;

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be greater than zero")
    private Double amount;

    @CreationTimestamp
    private LocalDateTime paymentTime;

    public Payment() {
    }

    public Payment(PaymentBuilder builder) {
        this.id = builder.id;
        this.paymentMethod = builder.paymentMethod;
        this.rideId = builder.rideId;
        this.driverId = builder.driverId;
        this.riderId = builder.riderId;
        this.paymentStatus = builder.paymentStatus;
        this.amount = builder.amount;
        this.paymentTime = builder.paymentTime;
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

    public static class PaymentBuilder {
        private Long id;
        private PaymentMethod paymentMethod;
        private Long rideId;
        private Long driverId;
        private Long riderId;
        private PaymentStatus paymentStatus;
        private Double amount;
        private LocalDateTime paymentTime;

        public PaymentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public PaymentBuilder paymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
            return this;
        }

        public PaymentBuilder rideId(Long rideId) {
            this.rideId = rideId;
            return this;
        }

        public PaymentBuilder driverId(Long driverId) {
            this.driverId = driverId;
            return this;
        }

        public PaymentBuilder riderId(Long riderId) {
            this.riderId = riderId;
            return this;
        }

        public PaymentBuilder paymentStatus(PaymentStatus paymentStatus) {
            this.paymentStatus = paymentStatus;
            return this;
        }

        public PaymentBuilder amount(Double amount) {
            this.amount = amount;
            return this;
        }

        public PaymentBuilder paymentTime(LocalDateTime paymentTime) {
            this.paymentTime = paymentTime;
            return this;
        }

        public Payment build() {
            return new Payment(this);
        }
    }
}
