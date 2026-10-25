package com.project.ridemesh.booking.dto;

public class DeclineBookingDto {
    private Long workerId;
    private String reason;

    public DeclineBookingDto() {
    }

    public DeclineBookingDto(Long workerId, String reason) {
        this.workerId = workerId;
        this.reason = reason;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
