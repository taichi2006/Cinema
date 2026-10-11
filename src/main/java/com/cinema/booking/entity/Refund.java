package com.cinema.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds", schema = "cinema")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refund_id")
    private Integer refundId;

    @Column(name = "payment_id", nullable = false)
    private Integer paymentId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, PROCESSING, COMPLETED, FAILED

    @Column(name = "gateway_refund_id", length = 100)
    private String gatewayRefundId;

    @Column(name = "refund_date", insertable = false, updatable = false)
    private LocalDateTime refundDate;

    public Refund() {}

    public Refund(Integer paymentId, BigDecimal amount, String reason, String status) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
    }

    public Integer getRefundId() { return refundId; }
    public void setRefundId(Integer refundId) { this.refundId = refundId; }

    public Integer getPaymentId() { return paymentId; }
    public void setPaymentId(Integer paymentId) { this.paymentId = paymentId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getGatewayRefundId() { return gatewayRefundId; }
    public void setGatewayRefundId(String gatewayRefundId) { this.gatewayRefundId = gatewayRefundId; }

    public LocalDateTime getRefundDate() { return refundDate; }
    public void setRefundDate(LocalDateTime refundDate) { this.refundDate = refundDate; }
}
