package com.cinema.booking.dto.response;

public class CancelBookingResponse {
    private Long bookingId;
    private String status;
    private String cancelledAt;

    public CancelBookingResponse() {}

    public CancelBookingResponse(Long bookingId, String status, String cancelledAt) {
        this.bookingId = bookingId;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }
}
