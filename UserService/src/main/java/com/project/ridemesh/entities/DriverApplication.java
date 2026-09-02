package com.project.ridemesh.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_application")
public class DriverApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private String riderName;
    private String vehicleModel;
    private String vehicleId;
    private String licenseNumber;
    
    @Column(length = 500)
    private String licensePhotoUrl;

    private String vehicleType;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
    private String state;
    private String district;
    
    @Column(length = 2000)
    private String vehiclePhotos;

    @Column(length = 500)
    private String identityPhotoUrl;

    private String identityDocType;

    @Column(nullable = false)
    private String status = "PENDING";

    private LocalDateTime submittedTime = LocalDateTime.now();

    public DriverApplication() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRiderName() {
        return riderName;
    }

    public void setRiderName(String riderName) {
        this.riderName = riderName;
    }

    public String getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(String vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getLicensePhotoUrl() {
        return licensePhotoUrl;
    }

    public void setLicensePhotoUrl(String licensePhotoUrl) {
        this.licensePhotoUrl = licensePhotoUrl;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSubmittedTime() {
        return submittedTime;
    }

    public void setSubmittedTime(LocalDateTime submittedTime) {
        this.submittedTime = submittedTime;
    }

    public String getVehiclePhotos() {
        return vehiclePhotos;
    }

    public void setVehiclePhotos(String vehiclePhotos) {
        this.vehiclePhotos = vehiclePhotos;
    }

    public String getIdentityPhotoUrl() {
        return identityPhotoUrl;
    }

    public void setIdentityPhotoUrl(String identityPhotoUrl) {
        this.identityPhotoUrl = identityPhotoUrl;
    }

    public String getIdentityDocType() {
        return identityDocType;
    }

    public void setIdentityDocType(String identityDocType) {
        this.identityDocType = identityDocType;
    }
}
