package com.project.ridemesh.booking.dto;

import java.time.LocalDateTime;

public class BookingApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public BookingApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public BookingApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> BookingApiResponse<T> success(T data, String message) {
        return new BookingApiResponse<>(true, message, data);
    }

    public static <T> BookingApiResponse<T> success(T data) {
        return success(data, "Operation completed successfully");
    }

    public static <T> BookingApiResponse<T> error(String message) {
        return new BookingApiResponse<>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
