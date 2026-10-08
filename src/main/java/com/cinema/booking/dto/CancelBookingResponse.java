package com.cinema.booking.dto;

public class CancelBookingResponse {
    private Integer bookingId;
    private String status;
    private String cancelledAt;

    public CancelBookingResponse() {}

    public CancelBookingResponse(Integer bookingId, String status, String cancelledAt) {
        this.bookingId = bookingId;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }

    public Integer getBookingId() { return bookingId; }
    public void setBookingId(Integer bookingId) { this.bookingId = bookingId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }
}
