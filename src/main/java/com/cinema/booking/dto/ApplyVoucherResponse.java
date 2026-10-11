package com.cinema.booking.dto;

import java.math.BigDecimal;

public class ApplyVoucherResponse {
    private Long bookingId;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String voucherApplied;

    public ApplyVoucherResponse() {}

    public ApplyVoucherResponse(Long bookingId, BigDecimal discountAmount, BigDecimal finalAmount, String voucherApplied) {
        this.bookingId = bookingId;
        this.discountAmount = discountAmount;
        this.finalAmount = finalAmount;
        this.voucherApplied = voucherApplied;
    }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

    public String getVoucherApplied() { return voucherApplied; }
    public void setVoucherApplied(String voucherApplied) { this.voucherApplied = voucherApplied; }
}
