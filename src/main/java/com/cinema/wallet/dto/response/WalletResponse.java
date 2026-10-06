package com.cinema.wallet.dto.response;

/**
 * Response schema cho thông tin số dư ví (GET /wallet).
 */
public class WalletResponse {

    private String id;
    private Long balance;
    private String currency;
    private String updatedAt;

    public WalletResponse() {}

    public WalletResponse(String id, Long balance, String currency, String updatedAt) {
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

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
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

