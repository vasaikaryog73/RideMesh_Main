package com.project.ridemesh.dto;

public class RatingDto {
    private Double rating;
    private Double driverRating;
    private Double riderRating;
    private Long driverId;
    private Long rideId;

    public RatingDto() {
    }

    public RatingDto(Double rating) {
        this.rating = rating;
        this.driverRating = rating;
    }

    public RatingDto(Double rating, Long driverId) {
        this.rating = rating;
        this.driverRating = rating;
        this.driverId = driverId;
    }

    public Double getRating() {
        return rating != null ? rating : driverRating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
        if (this.driverRating == null) {
            this.driverRating = rating;
        }
    }

    public Double getDriverRating() {
        return driverRating != null ? driverRating : rating;
    }

    public void setDriverRating(Double driverRating) {
        this.driverRating = driverRating;
        if (this.rating == null) {
            this.rating = driverRating;
        }
    }

    public Double getRiderRating() {
        return riderRating;
    }

    public void setRiderRating(Double riderRating) {
        this.riderRating = riderRating;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public Long getRideId() {
        return rideId;
    }

    public void setRideId(Long rideId) {
        this.rideId = rideId;
    }
}

