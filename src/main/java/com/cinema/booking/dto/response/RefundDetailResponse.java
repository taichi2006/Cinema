package com.cinema.booking.dto.response;

import java.math.BigDecimal;

public class RefundDetailResponse {
    private Long refundId;
    private Long bookingId;
    private BigDecimal refundAmount;
    private String status;
    private String refundedAt;

    public RefundDetailResponse() {}

    public RefundDetailResponse(Long refundId, Long bookingId, BigDecimal refundAmount, String status, String refundedAt) {
        this.refundId = refundId;
        this.bookingId = bookingId;
        this.refundAmount = refundAmount;
        this.status = status;
        this.refundedAt = refundedAt;
    }

    public Long getRefundId() { return refundId; }
    public void setRefundId(Long refundId) { this.refundId = refundId; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRefundedAt() { return refundedAt; }
    public void setRefundedAt(String refundedAt) { this.refundedAt = refundedAt; }
}
