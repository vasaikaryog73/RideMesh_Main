package com.project.ridemesh.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(indexes = {
        @Index(name = "idx_rating_ride_unique", columnList = "rideId", unique = true),
        @Index(name = "idx_rating_rider", columnList = "riderId"),
        @Index(name = "idx_rating_driver", columnList = "driverId")
})
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Ride ID is required")
    @Column(unique = true, nullable = false)
    private Long rideId;

    @NotNull(message = "Rider ID is required")
    private Long riderId;

    @NotNull(message = "Driver ID is required")
    private Long driverId;

    @NotNull(message = "Rating is required")
    @Min(value = 0, message = "Rating cannot be less than 0")
    @Max(value = 5, message = "Rating cannot be more than 5")
    private Double riderRating;

    @NotNull(message = "Rating is required")
    @Min(value = 0, message = "Rating cannot be less than 0")
    @Max(value = 5, message = "Rating cannot be more than 5")
    private Double driverRating;

    public Rating() {
    }

    public Rating(RatingBuilder builder) {
        this.id = builder.id;
        this.rideId = builder.rideId;
        this.riderId = builder.riderId;
        this.driverId = builder.driverId;
        this.riderRating = builder.riderRating;
        this.driverRating = builder.driverRating;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRideId() {
        return rideId;
    }

    public void setRideId(Long rideId) {
        this.rideId = rideId;
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

    public Double getRiderRating() {
        return riderRating;
    }

    public void setRiderRating(Double riderRating) {
        this.riderRating = riderRating;
    }

    public Double getDriverRating() {
        return driverRating;
    }

    public void setDriverRating(Double driverRating) {
        this.driverRating = driverRating;
    }

    public static class RatingBuilder {
        private Long id;
        private Long rideId;
        private Long riderId;
        private Long driverId;
        private Double riderRating;
        private Double driverRating;

        public RatingBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public RatingBuilder rideId(Long rideId) {
            this.rideId = rideId;
            return this;
        }

        public RatingBuilder riderId(Long riderId) {
            this.riderId = riderId;
            return this;
        }

        public RatingBuilder driverId(Long driverId) {
            this.driverId = driverId;
            return this;
        }

        public RatingBuilder riderRating(Double riderRating) {
            this.riderRating = riderRating;
            return this;
        }

        public RatingBuilder driverRating(Double driverRating) {
            this.driverRating = driverRating;
            return this;
        }

        public Rating build() {
            return new Rating(this);
        }
    }
}
