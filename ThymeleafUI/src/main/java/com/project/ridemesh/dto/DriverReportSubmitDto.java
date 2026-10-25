package com.project.ridemesh.dto;

public class DriverReportSubmitDto {

    private Long rideId;
    private Long driverId;
    private String reason;
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
