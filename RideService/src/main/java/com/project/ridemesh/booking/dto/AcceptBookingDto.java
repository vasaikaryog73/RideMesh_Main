package com.project.ridemesh.booking.dto;

public class AcceptBookingDto {
    private Long workerId;
    private String workerName;

    public AcceptBookingDto() {
    }

    public AcceptBookingDto(Long workerId, String workerName) {
        this.workerId = workerId;
        this.workerName = workerName;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }
}
