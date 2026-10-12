package com.cinema.booking.dto.response;

import java.math.BigDecimal;

public class CreateRefundResponse {
    private Long refundId;
    private Long bookingId;
    private BigDecimal refundAmount;
    private String status;
    private String requestedAt;

    public CreateRefundResponse() {}

    public CreateRefundResponse(Long refundId, Long bookingId, BigDecimal refundAmount, String status, String requestedAt) {
        this.refundId = refundId;
        this.bookingId = bookingId;
        this.refundAmount = refundAmount;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    public Long getRefundId() { return refundId; }
    public void setRefundId(Long refundId) { this.refundId = refundId; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestedAt() { return requestedAt; }
    public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }
}
