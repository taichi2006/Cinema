package com.cinema.common.dto;

public class ErrorResponse {
    private boolean success = false;
    private int status;
    private String error;

    public ErrorResponse(int status, String error) {
        this.status = status;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }
}
