package com.project.ridemesh.dto;

import com.project.ridemesh.dto.PointDto;

public class DriverDto {
    private Long id;
    private UserDto user;
    private Double rating;
    private Boolean available;
    private Long vehicleId;
    private PointDto currentLocation;
    private String vehicleType;
    private String phoneNumber;
    private String state;
    private String district;

    public DriverDto() {
    }

    public DriverDto(Long id, UserDto user, Double rating, Boolean available, Long vehicleId) {
        this.id = id;
        this.user = user;
        this.rating = rating;
        this.available = available;
        this.vehicleId = vehicleId;
    }

    public DriverDto(Long id, UserDto user, Double rating, Boolean available, Long vehicleId, PointDto currentLocation, String vehicleType, String phoneNumber, String state, String district) {
        this.id = id;
        this.user = user;
        this.rating = rating;
        this.available = available;
        this.vehicleId = vehicleId;
        this.currentLocation = currentLocation;
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

    public Long getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(Long vehicleId) {
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
