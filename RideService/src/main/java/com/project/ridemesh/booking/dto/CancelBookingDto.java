package com.project.ridemesh.booking.dto;

public class CancelBookingDto {
    private Long customerId;
    private String reason;

    public CancelBookingDto() {
    }

    public CancelBookingDto(Long customerId, String reason) {
        this.customerId = customerId;
        this.reason = reason;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
