package com.cinema.payment.dto;

import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentMethod;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {
    private Long paymentId;
    private Long bookingId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private PaymentType paymentType;
    private LocalDateTime paymentDate;
    private LocalDateTime expiredAt;
    private String gatewayTransactionId;
    private String failureReason;

    public PaymentResponse() {
    }

    public PaymentResponse(Payment payment) {
        this.paymentId = payment.getId();
        this.bookingId = payment.getBookingId();
        this.amount = payment.getAmount();
        this.method = payment.getMethod();
        this.status = payment.getStatus();
        this.paymentType = payment.getPaymentType();
        this.paymentDate = payment.getPaymentDate();
        this.expiredAt = payment.getExpiredAt();
        this.gatewayTransactionId = payment.getGatewayTransactionId();
        this.failureReason = payment.getFailureReason();
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
    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public LocalDateTime getExpiredAt() { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt) { this.expiredAt = expiredAt; }
    public String getGatewayTransactionId() { return gatewayTransactionId; }
    public void setGatewayTransactionId(String gatewayTransactionId) { this.gatewayTransactionId = gatewayTransactionId; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}
