package com.cinema.wallet.dto.response;

/**
 * Response schema cho thông tin số dư ví (GET /wallet).
 */
public class WalletResponse {

    private String id;
    private Long balance;

    public WalletResponse() {}

    public WalletResponse(String id, Long balance) {
        this.id = id;
        this.balance = balance;
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

}

