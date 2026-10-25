package com.project.ridemesh.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse<T> {

    private LocalDateTime timeStamp;
    private T data;
    private Object error;
    private Object apiError;

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

    public Object getApiError() {
        return apiError;
    }

    public void setApiError(Object apiError) {
        this.apiError = apiError;
    }
}
