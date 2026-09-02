package com.project.ridemesh.dto;

import com.project.ridemesh.entities.enums.PaymentMethod;
import com.project.ridemesh.entities.enums.RideRequestStatus;
import java.time.LocalDateTime;

public class RideRequestDto {

    private Long id;
    private PointDto pickUpLocation;
    private PointDto dropOffLocation;
    private LocalDateTime requestTime;
    private RiderDto rider;
    private Double fare;
    private PaymentMethod paymentMethod;
    private RideRequestStatus status;
    private String vehicleType;
    private String pickupState;
    private String pickupDistrict;

    public RideRequestDto() {
    }

    public RideRequestDto(Long id, PointDto pickUpLocation, Double fare, PointDto dropOffLocation, LocalDateTime requestTime, RiderDto rider, PaymentMethod paymentMethod, RideRequestStatus status) {
        this.id = id;
        this.pickUpLocation = pickUpLocation;
        this.dropOffLocation = dropOffLocation;
        this.requestTime = requestTime;
        this.fare = fare;
        this.rider = rider;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    public RideRequestDto(Long id, PointDto pickUpLocation, Double fare, PointDto dropOffLocation, LocalDateTime requestTime, RiderDto rider, PaymentMethod paymentMethod, RideRequestStatus status, String vehicleType) {
        this.id = id;
        this.pickUpLocation = pickUpLocation;
        this.dropOffLocation = dropOffLocation;
        this.requestTime = requestTime;
        this.fare = fare;
        this.rider = rider;
        this.paymentMethod = paymentMethod;
        this.status = status;
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

    public PointDto getPickUpLocation() {
        return pickUpLocation;
    }

    public void setPickUpLocation(PointDto pickUpLocation) {
        this.pickUpLocation = pickUpLocation;
    }

    public PointDto getDropOffLocation() {
        return dropOffLocation;
    }

    public void setDropOffLocation(PointDto dropOffLocation) {
        this.dropOffLocation = dropOffLocation;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(LocalDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public RiderDto getRider() {
        return rider;
    }

    public void setRider(RiderDto rider) {
        this.rider = rider;
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
}
