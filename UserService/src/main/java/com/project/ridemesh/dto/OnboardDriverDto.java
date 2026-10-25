package com.project.ridemesh.dto;

public class OnboardDriverDto {
    private String vehicleId;
    private PointDto currentLocation;
    private String vehicleType;
    private String phoneNumber;
    private String state;
    private String district;

    public OnboardDriverDto() {
    }

    public OnboardDriverDto(String vehicleId, PointDto currentLocation) {
        this.vehicleId = vehicleId;
        this.currentLocation = currentLocation;
    }

    public OnboardDriverDto(String vehicleId, PointDto currentLocation, String vehicleType, String phoneNumber, String state, String district) {
        this.vehicleId = vehicleId;
        this.currentLocation = currentLocation;
        this.vehicleType = vehicleType;
        this.phoneNumber = phoneNumber;
        this.state = state;
        this.district = district;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public PointDto getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(PointDto currentLocation) {
        this.currentLocation = currentLocation;
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
}
