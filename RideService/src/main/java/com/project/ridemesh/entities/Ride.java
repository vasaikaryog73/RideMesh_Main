package com.project.ridemesh.entities;

import com.project.ridemesh.entities.enums.PaymentMethod;
import com.project.ridemesh.entities.enums.RideStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_ride_rider", columnList = "riderId"),
        @Index(name = "idx_ride_driver", columnList = "driverId")
})
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Pick-up location is required")
    @Column(columnDefinition = "Geometry(Point, 4326)")
    private Point pickUpLocation;

    @NotNull(message = "Drop-off location is required")
    @Column(columnDefinition = "Geometry(Point, 4326)")
    private Point dropOffLocation;

    @CreationTimestamp
    private LocalDateTime createdTime;

    @NotNull(message = "Rider ID is required")
    private Long riderId;

    @NotNull(message = "Driver ID is required")
    private Long driverId;

    @NotNull(message = "Payment method is required")
    @Enumerated(value = EnumType.STRING)
    private PaymentMethod paymentMethod;

    @NotNull(message = "Ride status is required")
    @Enumerated(value = EnumType.STRING)
    private RideStatus status;

    @NotNull(message = "Fare is required")
    @Positive(message = "Fare must be greater than zero")
    private Double fare;

    @NotBlank(message = "OTP is required")
    @Size(min = 4, max = 6, message = "OTP must be between 4 and 6 characters")
    private String otp;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String vehicleType;
    private Boolean riderCompleted = false;
    private Boolean driverCompleted = false;
    private Boolean cashPaid = false;
    private Boolean cashReceived = false;
    private String pickupState;
    private String pickupDistrict;
    private String dropoffState;
    private String dropoffDistrict;
    @Column(length = 1000)
    private String pickupAddress;
    @Column(length = 1000)
    private String dropoffAddress;
    private Double driverRating;

    public Ride() {
    }

    public Ride(Long id, Point pickUpLocation, Point dropOffLocation, LocalDateTime createdTime, Long riderId, Long driverId, PaymentMethod paymentMethod, RideStatus status, Double fare, LocalDateTime startedAt, LocalDateTime endedAt, String otp) {
        this.id = id;
        this.pickUpLocation = pickUpLocation;
        this.dropOffLocation = dropOffLocation;
        this.createdTime = createdTime;
        this.riderId = riderId;
        this.driverId = driverId;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.fare = fare;
        this.otp = otp;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
    }

    public Ride(Long id, Point pickUpLocation, Point dropOffLocation, LocalDateTime createdTime, Long riderId, Long driverId, PaymentMethod paymentMethod, RideStatus status, Double fare, LocalDateTime startedAt, LocalDateTime endedAt, String otp, String vehicleType, Boolean riderCompleted, Boolean driverCompleted) {
        this.id = id;
        this.pickUpLocation = pickUpLocation;
        this.dropOffLocation = dropOffLocation;
        this.createdTime = createdTime;
        this.riderId = riderId;
        this.driverId = driverId;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.fare = fare;
        this.otp = otp;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.vehicleType = vehicleType;
        this.riderCompleted = riderCompleted;
        this.driverCompleted = driverCompleted;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public Boolean getRiderCompleted() {
        return riderCompleted;
    }

    public void setRiderCompleted(Boolean riderCompleted) {
        this.riderCompleted = riderCompleted;
    }

    public Boolean getDriverCompleted() {
        return driverCompleted;
    }

    public void setDriverCompleted(Boolean driverCompleted) {
        this.driverCompleted = driverCompleted;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Point getPickUpLocation() {
        return pickUpLocation;
    }

    public void setPickUpLocation(Point pickUpLocation) {
        this.pickUpLocation = pickUpLocation;
    }

    public Point getDropOffLocation() {
        return dropOffLocation;
    }

    public void setDropOffLocation(Point dropOffLocation) {
        this.dropOffLocation = dropOffLocation;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public Long getRiderId() {
        return riderId;
    }

    public void setRiderId(Long riderId) {
        this.riderId = riderId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public Double getFare() {
        return fare;
    }

    public void setFare(Double fare) {
        this.fare = fare;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getPickupState() {
        return pickupState;
    }

    public void setPickupState(String pickupState) {
        this.pickupState = pickupState;
    }

    public String getPickupDistrict() {
        return pickupDistrict;
    }

    public void setPickupDistrict(String pickupDistrict) {
        this.pickupDistrict = pickupDistrict;
    }

    public Boolean getCashPaid() {
        return cashPaid;
    }

    public void setCashPaid(Boolean cashPaid) {
        this.cashPaid = cashPaid;
    }

    public Boolean getCashReceived() {
        return cashReceived;
    }

    public void setCashReceived(Boolean cashReceived) {
        this.cashReceived = cashReceived;
    }

    public String getDropoffState() {
        return dropoffState;
    }

    public void setDropoffState(String dropoffState) {
        this.dropoffState = dropoffState;
    }

    public String getDropoffDistrict() {
        return dropoffDistrict;
    }

    public void setDropoffDistrict(String dropoffDistrict) {
        this.dropoffDistrict = dropoffDistrict;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public Double getDriverRating() {
        return driverRating;
    }

    public void setDriverRating(Double driverRating) {
        this.driverRating = driverRating;
    }
}
