package com.cinema.wallet.dto.response;

import java.math.BigDecimal;

/**
 * Response schema cho danh sách yêu cầu nạp tiền chờ duyệt của Admin (GET /wallet/top-up/pending).
 */
public class AdminPendingItemResponse {

    private String id;
    private Long walletId;
    private Long userId;
    private String userEmail;
    private BigDecimal amount;
    private String currency;
    private String method;
    private String status;
    private String description;
    private String createdAt;
    private String expiresAt;

    public AdminPendingItemResponse() {}

    public AdminPendingItemResponse(String id, Long walletId, Long userId, String userEmail, BigDecimal amount,
                                   String currency, String method, String status, String description,
                                   String createdAt, String expiresAt) {
        this.id = id;
        this.walletId = walletId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.amount = amount;
        this.currency = currency;
        this.method = method;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}
