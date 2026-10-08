package com.cinema.booking.dto;

import java.math.BigDecimal;
import java.util.List;

public class CreateBookingResponse {
    private Integer bookingId;
    private Integer showTimeId;
    private List<Integer> seatIds;
    private BigDecimal totalAmount;
    private String holdExpiresAt;
    private String status;

    public CreateBookingResponse() {}

    public CreateBookingResponse(Integer bookingId, Integer showTimeId, List<Integer> seatIds, BigDecimal totalAmount, String holdExpiresAt, String status) {
        this.bookingId = bookingId;
        this.showTimeId = showTimeId;
        this.seatIds = seatIds;
        this.totalAmount = totalAmount;
        this.holdExpiresAt = holdExpiresAt;
        this.status = status;
    }

    public Integer getBookingId() { return bookingId; }
    public void setBookingId(Integer bookingId) { this.bookingId = bookingId; }

    public Integer getShowTimeId() { return showTimeId; }
    public void setShowTimeId(Integer showTimeId) { this.showTimeId = showTimeId; }

    public List<Integer> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Integer> seatIds) { this.seatIds = seatIds; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getHoldExpiresAt() { return holdExpiresAt; }
    public void setHoldExpiresAt(String holdExpiresAt) { this.holdExpiresAt = holdExpiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
