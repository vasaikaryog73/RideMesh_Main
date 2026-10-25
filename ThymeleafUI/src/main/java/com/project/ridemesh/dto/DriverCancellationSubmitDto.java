package com.project.ridemesh.dto;

public class DriverCancellationSubmitDto {

    private String otp;
    private String reason;
    private String mediaUrl;

    public DriverCancellationSubmitDto() {
    }

    public DriverCancellationSubmitDto(String otp, String reason, String mediaUrl) {
        this.otp = otp;
        this.reason = reason;
        this.mediaUrl = mediaUrl;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
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
