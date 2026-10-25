package com.project.ridemesh.dto;

import com.project.ridemesh.entities.enums.PaymentMethod;
import com.project.ridemesh.entities.enums.RideStatus;
import java.time.LocalDateTime;

public class RideDto {

    private Long id;
    private PointDto pickUpLocation;
    private PointDto dropOffLocation;
    private LocalDateTime createdTime;
    private RiderDto rider;
    private DriverDto driver;
    private PaymentMethod paymentMethod;
    private RideStatus status;
    private Double fare;
    private String otp;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String vehicleType;
    private Boolean riderCompleted;
    private Boolean driverCompleted;
    private Boolean cashPaid;
    private Boolean cashReceived;
    private String pickupState;
    private String pickupDistrict;
    private String dropoffState;
    private String dropoffDistrict;
    private String pickupAddress;
    private String dropoffAddress;
    private Double driverRating;

    public RideDto() {
    }

    public RideDto(Long id, PointDto pickUpLocation, RiderDto rider, String otp, DriverDto driver, PointDto dropOffLocation, LocalDateTime createdTime, PaymentMethod paymentMethod, RideStatus status, Double fare, LocalDateTime startedAt, LocalDateTime endedAt) {
        this.id = id;
        this.pickUpLocation = pickUpLocation;
        this.dropOffLocation = dropOffLocation;
        this.createdTime = createdTime;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.fare = fare;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.rider = rider;
        this.driver = driver;
        this.otp = otp;
    }

    public RideDto(Long id, PointDto pickUpLocation, RiderDto rider, String otp, DriverDto driver, PointDto dropOffLocation, LocalDateTime createdTime, PaymentMethod paymentMethod, RideStatus status, Double fare, LocalDateTime startedAt, LocalDateTime endedAt, String vehicleType, Boolean riderCompleted, Boolean driverCompleted) {
        this.id = id;
        this.pickUpLocation = pickUpLocation;
        this.dropOffLocation = dropOffLocation;
        this.createdTime = createdTime;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.fare = fare;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.rider = rider;
        this.driver = driver;
        this.otp = otp;
        this.vehicleType = vehicleType;
        this.riderCompleted = riderCompleted;
        this.driverCompleted = driverCompleted;
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

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
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

    public RiderDto getRider() {
        return rider;
    }

    public void setRider(RiderDto rider) {
        this.rider = rider;
    }

    public DriverDto getDriver() {
        return driver;
    }

    public void setDriver(DriverDto driver) {
        this.driver = driver;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
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
