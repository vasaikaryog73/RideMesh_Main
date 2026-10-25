package com.project.ridemesh.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.locationtech.jts.geom.Point;

@Entity
@Table(indexes = {
        @Index(name = "idx_driver_vehicle_id", columnList = "vehicleId"),
        @Index(name = "idx_driver_user", columnList = "user_id")
})
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    @NotNull(message = "User must be associated with the driver")
    private User user;

    @NotNull(message = "Rating is required")
    @Min(value = 0, message = "Rating cannot be less than 0")
    @Max(value = 5, message = "Rating cannot be more than 5")
    private Double rating;

    @NotNull(message = "Availability status is required")
    private Boolean available;

    @Column(columnDefinition = "Geometry(Point, 4326)")
    private Point currentLocation;

    private String vehicleId;

    private String vehicleType;
    private String phoneNumber;
    private String state;
    private String district;

    public Driver() {
    }

    public Driver(DriverBuilder builder) {
        this.id = builder.id;
        this.user = builder.user;
        this.rating = builder.rating;
        this.available = builder.available;
        this.currentLocation = builder.currentLocation;
        this.vehicleId = builder.vehicleId;
        this.vehicleType = builder.vehicleType;
        this.phoneNumber = builder.phoneNumber;
        this.state = builder.state;
        this.district = builder.district;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
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

    public Point getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Point currentLocation) {
        this.currentLocation = currentLocation;
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

    public static class DriverBuilder {

        private Long id;
        private User user;
        private Double rating;
        private Boolean available;
        private Point currentLocation;
        private String vehicleId;
        private String vehicleType;
        private String phoneNumber;
        private String state;
        private String district;

        public DriverBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public DriverBuilder user(User user) {
            this.user = user;
            return this;
        }

        public DriverBuilder rating(Double rating) {
            this.rating = rating;
            return this;
        }

        public DriverBuilder available(Boolean available) {
            this.available = available;
            return this;
        }

        public DriverBuilder currentLocation(Point currentLocation) {
            this.currentLocation = currentLocation;
            return this;
        }

        public DriverBuilder vehicleId(String vehicleId) {
            this.vehicleId = vehicleId;
            return this;
        }

        public DriverBuilder vehicleType(String vehicleType) {
            this.vehicleType = vehicleType;
            return this;
        }

        public DriverBuilder phoneNumber(String phoneNumber) {
            this.phoneNumber = phoneNumber;
            return this;
        }

        public DriverBuilder state(String state) {
            this.state = state;
            return this;
        }

        public DriverBuilder district(String district) {
            this.district = district;
            return this;
        }

        public Driver build() {
            return new Driver(this);
        }
    }
}
