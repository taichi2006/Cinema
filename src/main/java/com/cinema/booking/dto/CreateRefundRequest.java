package com.cinema.booking.dto;

public class CreateRefundRequest {
    private String reason;

    public CreateRefundRequest() {}

    public CreateRefundRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
