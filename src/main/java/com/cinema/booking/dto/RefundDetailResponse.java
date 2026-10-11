package com.cinema.booking.dto;

import java.math.BigDecimal;

public class RefundDetailResponse {
    private Integer refundId;
    private Integer bookingId;
    private BigDecimal refundAmount;
    private String status;
    private String refundedAt;

    public RefundDetailResponse() {}

    public RefundDetailResponse(Integer refundId, Integer bookingId, BigDecimal refundAmount, String status, String refundedAt) {
        this.refundId = refundId;
        this.bookingId = bookingId;
        this.refundAmount = refundAmount;
        this.status = status;
        this.refundedAt = refundedAt;
    }

    public Integer getRefundId() { return refundId; }
    public void setRefundId(Integer refundId) { this.refundId = refundId; }

    public Integer getBookingId() { return bookingId; }
    public void setBookingId(Integer bookingId) { this.bookingId = bookingId; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRefundedAt() { return refundedAt; }
    public void setRefundedAt(String refundedAt) { this.refundedAt = refundedAt; }
}
