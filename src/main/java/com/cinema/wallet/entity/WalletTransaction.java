package com.cinema.wallet.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Thực thể Bút toán giao dịch ví
 * Sổ cái biến động số dư
 */
@Entity
@Table(name = "wallet_transactions", schema = "cinema")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "transaction_type", nullable = false, length = 10)
    private String transactionType;

    @Column(nullable = false, length = 6)
    private String direction; // "CREDIT" hoặc "DEBIT"

    @Column(nullable = false)
    private Long amount;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(length = 3, nullable = false)
    private String currency = "VND";

    @Column(name = "topup_id")
    private Long topupId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "refund_id")
    private Long refundId;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public WalletTransaction() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getTopupId() {
        return topupId;
    }

    public void setTopupId(Long topupId) {
        this.topupId = topupId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Long getRefundId() {
        return refundId;
    }

    public void setRefundId(Long refundId) {
        this.refundId = refundId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

