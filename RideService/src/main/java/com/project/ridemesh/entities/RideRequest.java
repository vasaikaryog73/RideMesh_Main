package com.project.ridemesh.entities;

import com.project.ridemesh.entities.enums.PaymentMethod;
import com.project.ridemesh.entities.enums.RideRequestStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_ride_request_rider", columnList = "riderId")
})
public class RideRequest {

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
    private LocalDateTime requestTime;

    @NotNull(message = "Rider ID is required")
    private Long riderId;

    @NotNull(message = "Fare is required")
    @Positive(message = "Fare must be greater than zero")
    private Double fare;

    @NotNull(message = "Payment method is required")
    @Enumerated(value = EnumType.STRING)
    private PaymentMethod paymentMethod;

    @NotNull(message = "Request status is required")
    @Enumerated(value = EnumType.STRING)
    private RideRequestStatus status;

    private String vehicleType;
    private String pickupState;
    private String pickupDistrict;
    private String dropoffState;
    private String dropoffDistrict;
    @Column(length = 1000)
    private String pickupAddress;
    @Column(length = 1000)
    private String dropoffAddress;

    public RideRequest() {
    }

    public RideRequest(Long id, Double fare, Point pickUpLocation, Point dropOffLocation, LocalDateTime requestTime, Long riderId, PaymentMethod paymentMethod, RideRequestStatus status) {
        this.id = id;
        this.pickUpLocation = pickUpLocation;
        this.dropOffLocation = dropOffLocation;
        this.requestTime = requestTime;
        this.riderId = riderId;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.fare = fare;
    }

    public RideRequest(Long id, Double fare, Point pickUpLocation, Point dropOffLocation, LocalDateTime requestTime, Long riderId, PaymentMethod paymentMethod, RideRequestStatus status, String vehicleType) {
        this.id = id;
        this.pickUpLocation = pickUpLocation;
        this.dropOffLocation = dropOffLocation;
        this.requestTime = requestTime;
        this.riderId = riderId;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.fare = fare;
        this.vehicleType = vehicleType;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
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

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public Long getRiderId() {
        return riderId;
    }

    public void setRiderId(Long riderId) {
        this.riderId = riderId;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public RideRequestStatus getStatus() {
        return status;
    }

    public void setStatus(RideRequestStatus status) {
        this.status = status;
    }

    public Double getFare() {
        return fare;
    }

    public void setFare(Double fare) {
        this.fare = fare;
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
}
