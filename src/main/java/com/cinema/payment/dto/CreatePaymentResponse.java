package com.cinema.payment.dto;

import com.cinema.payment.enums.PaymentMethod;
import com.cinema.payment.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreatePaymentResponse {
    private Long paymentId;
    private Long bookingId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String paymentUrl;
    private LocalDateTime expiresAt;

    public CreatePaymentResponse() {
    }

    public CreatePaymentResponse(Long paymentId, Long bookingId, BigDecimal amount,
                                 PaymentMethod method, PaymentStatus status,
                                 String paymentUrl, LocalDateTime expiresAt) {
        this.paymentId = paymentId;
        this.bookingId = bookingId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.paymentUrl = paymentUrl;
        this.expiresAt = expiresAt;
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String paymentUrl) { this.paymentUrl = paymentUrl; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
