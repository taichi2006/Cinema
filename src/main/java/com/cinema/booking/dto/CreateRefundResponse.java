package com.cinema.booking.dto;

import java.math.BigDecimal;

public class CreateRefundResponse {
    private Integer refundId;
    private Integer bookingId;
    private BigDecimal refundAmount;
    private String status;
    private String requestedAt;

    public CreateRefundResponse() {}

    public CreateRefundResponse(Integer refundId, Integer bookingId, BigDecimal refundAmount, String status, String requestedAt) {
        this.refundId = refundId;
        this.bookingId = bookingId;
        this.refundAmount = refundAmount;
        this.status = status;
        this.requestedAt = requestedAt;
    }

    public Integer getRefundId() { return refundId; }
    public void setRefundId(Integer refundId) { this.refundId = refundId; }

    public Integer getBookingId() { return bookingId; }
    public void setBookingId(Integer bookingId) { this.bookingId = bookingId; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestedAt() { return requestedAt; }
    public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }
}
