package com.cinema.wallet.dto.response;

import java.math.BigDecimal;

// Response schema cho kết quả tạo yêu cầu nạp tiền (POST /wallet/top-up)
public class TopUpResponse {

    private Long paymentId;
    private Long bookingId;
    private BigDecimal amount;
    private String method;
    private String status;
    private String paymentType = "TOPUP";
    private String paymentDate;
    private String expiredAt;
    private String gatewayTransactionId;
    private Long walletTransactionId;
    private String failureReason;

    public TopUpResponse() {}

    public TopUpResponse(Long paymentId, BigDecimal amount, String method, String status,
                         Long walletTransactionId, String paymentDate) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.walletTransactionId = walletTransactionId;
        this.paymentDate = paymentDate;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(String expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public Long getWalletTransactionId() {
        return walletTransactionId;
    }

    public void setWalletTransactionId(Long walletTransactionId) {
        this.walletTransactionId = walletTransactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    // Helper alias
    public String getId() {
        return paymentId != null ? String.valueOf(paymentId) : (walletTransactionId != null ? String.valueOf(walletTransactionId) : null);
    }
}
