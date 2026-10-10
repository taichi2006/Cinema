package com.cinema.wallet.dto.response;

import com.cinema.wallet.enums.TransactionStatus;
import com.cinema.wallet.enums.TransactionType;
import java.math.BigDecimal;

// Response schema cho từng mục bút toán lịch sử giao dịch ví (GET /wallet/transactions)
public class TransactionItemResponse {

    private Long transactionId;
    private Long walletId;
    private BigDecimal amount;
    private TransactionType transactionType;
    private TransactionStatus status;
    private String description;
    private String createdAt;

    public TransactionItemResponse() {}

    public TransactionItemResponse(Long transactionId, Long walletId, BigDecimal amount,
                                   TransactionType transactionType, TransactionStatus status,
                                   String description, String createdAt) {
        this.transactionId = transactionId;
        this.walletId = walletId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.status = status;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
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

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
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

    // Helper getters cho tương thích
    public String getId() {
        return transactionId != null ? String.valueOf(transactionId) : null;
    }

    public String getType() {
        return transactionType != null ? transactionType.name() : null;
    }
}
