package com.cinema.wallet.dto.response;

import com.cinema.wallet.enums.WalletStatus;
import java.math.BigDecimal;

// Response schema cho thông tin số dư ví (GET /wallet)
public class WalletResponse {

    private Long walletId;
    private Long userId;
    private BigDecimal balance;
    private WalletStatus status;

    public WalletResponse() {}

    public WalletResponse(Long walletId, Long userId, BigDecimal balance, WalletStatus status) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
        this.status = status;
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

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public void setStatus(WalletStatus status) {
        this.status = status;
    }

    // Alias helper cho trường hợp cần id dạng chuỗi
    public String getId() {
        return walletId != null ? String.valueOf(walletId) : null;
    }
}
