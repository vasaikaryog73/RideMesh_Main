package com.project.ridemesh.dto;

import java.time.LocalDateTime;

public class DriverApplicationDto {
    private Long id;
    private Long userId;
    private String riderName;
    private String vehicleModel;
    private String vehicleId;
    private String licenseNumber;
    private String licensePhotoUrl;
    private String status;
    private LocalDateTime submittedTime;
    private String vehicleType;
    private String phoneNumber;
    private Double latitude;
    private Double longitude;
    private String state;
    private String district;
    private String vehiclePhotos;
    private String identityPhotoUrl;
    private String identityDocType;

    public DriverApplicationDto() {
    }

    public DriverApplicationDto(Long id, Long userId, String riderName, String vehicleModel, String vehicleId, String licenseNumber, String licensePhotoUrl, String status, LocalDateTime submittedTime) {
        this.id = id;
        this.userId = userId;
        this.riderName = riderName;
        this.vehicleModel = vehicleModel;
        this.vehicleId = vehicleId;
        this.licenseNumber = licenseNumber;
        this.licensePhotoUrl = licensePhotoUrl;
        this.status = status;
        this.submittedTime = submittedTime;
    }

    public DriverApplicationDto(Long id, Long userId, String riderName, String vehicleModel, String vehicleId, String licenseNumber, String licensePhotoUrl, String status, LocalDateTime submittedTime, String vehicleType, String phoneNumber, Double latitude, Double longitude) {
        this.id = id;
        this.userId = userId;
        this.riderName = riderName;
        this.vehicleModel = vehicleModel;
        this.vehicleId = vehicleId;
        this.licenseNumber = licenseNumber;
        this.licensePhotoUrl = licensePhotoUrl;
        this.status = status;
        this.submittedTime = submittedTime;
        this.vehicleType = vehicleType;
        this.phoneNumber = phoneNumber;
        this.latitude = latitude;
        this.longitude = longitude;
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
