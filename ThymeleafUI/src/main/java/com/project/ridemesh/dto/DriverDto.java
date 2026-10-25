package com.project.ridemesh.dto;

public class DriverDto {
    private Long id;
    private UserDto user;
    private Double rating;
    private Boolean available;
    private String vehicleId;
    private String vehicleType;
    private String phoneNumber;
    private String state;
    private String district;
    private String licenseNumber;
    private String licensePhotoUrl;
    private String vehiclePhotos;
    private String identityPhotoUrl;
    private String identityDocType;
    private Long reportsCount = 0L;

    public DriverDto() {
    }

    public DriverDto(Long id, UserDto user, Double rating, Boolean available, String vehicleId, String vehicleType, String phoneNumber, String state, String district) {
        this.id = id;
        this.user = user;
        this.rating = rating;
        this.available = available;
        this.vehicleId = vehicleId;
        this.vehicleType = vehicleType;
        this.phoneNumber = phoneNumber;
        this.state = state;
        this.district = district;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
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

    public Long getReportsCount() {
        return reportsCount;
    }

    public void setReportsCount(Long reportsCount) {
        this.reportsCount = reportsCount;
    }
}
