package com.cinema.wallet.dto.response;

import java.math.BigDecimal;

/**
 * Response schema cho thông tin số dư ví (GET /wallet).
 */
public class WalletResponse {

    private String id;
    private BigDecimal balance;
    private String currency;
    private String updatedAt;

    public WalletResponse() {}

    public WalletResponse(String id, BigDecimal balance, String currency, String updatedAt) {
        this.id = id;
        this.balance = balance;
        this.currency = currency;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
