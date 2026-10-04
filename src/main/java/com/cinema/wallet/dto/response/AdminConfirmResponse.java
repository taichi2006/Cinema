package com.cinema.wallet.dto.response;

import java.math.BigDecimal;

/**
 * Response schema sau khi Admin duyệt nạp tiền (POST /wallet/top-up/{id}/confirm).
 */
public class AdminConfirmResponse {

    private String id;
    private Long walletId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private BigDecimal newBalance;
    private String completedAt;

    public AdminConfirmResponse() {}

    public AdminConfirmResponse(String id, Long walletId, BigDecimal amount, String currency, String status,
                                BigDecimal newBalance, String completedAt) {
        this.id = id;
        this.walletId = walletId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.newBalance = newBalance;
        this.completedAt = completedAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(BigDecimal newBalance) {
        this.newBalance = newBalance;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }
}
