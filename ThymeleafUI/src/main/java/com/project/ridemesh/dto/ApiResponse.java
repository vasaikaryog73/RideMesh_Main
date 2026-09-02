package com.project.ridemesh.dto;

import java.time.LocalDateTime;

public class ApiResponse<T> {

    private LocalDateTime timeStamp;
    private T data;
    private Object error;

    public ApiResponse() {
    }

    public ApiResponse(T data) {
        this.data = data;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }
}
