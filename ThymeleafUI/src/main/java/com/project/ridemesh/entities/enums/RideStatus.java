package com.project.ridemesh.entities.enums;

public enum RideStatus {
    REQUESTED,
    ACCEPTED,
    REJECTED,
    IN_PROGRESS,
    PENDING_CUSTOMER_APPROVAL,
    COMPLETED,
    CANCELLED,

    // Legacy aliases
    CONFIRMED,
    ONGOING,
    ENDED,
    RIDER_RATED,
    PAYMENT_COMPLETED
}
