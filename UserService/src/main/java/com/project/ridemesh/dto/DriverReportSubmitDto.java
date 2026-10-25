package com.project.ridemesh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DriverReportSubmitDto {

    private Long rideId;

    @NotNull(message = "Driver ID is required")
    private Long driverId;

    @NotBlank(message = "Reason is required")
    private String reason;

    @NotBlank(message = "Evidence media is compulsory")
    private String mediaUrl;

    public DriverReportSubmitDto() {
    }

    public DriverReportSubmitDto(Long rideId, Long driverId, String reason, String mediaUrl) {
        this.rideId = rideId;
        this.driverId = driverId;
        this.reason = reason;
        this.mediaUrl = mediaUrl;
    }

    public Long getRideId() {
        return rideId;
    }

    public void setRideId(Long rideId) {
        this.rideId = rideId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }
}
