package com.cinema.wallet.entity;

import com.cinema.wallet.enums.WalletTopupStatus;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Thực thể Yêu cầu nạp tiền.
 * Quản lý vòng đời của yêu cầu nạp tiền.
 */
@Entity
@Table(name = "wallet_topups", schema = "cinema")
public class WalletTopup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "topup_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false)
    private Long amount;

    @Column(length = 3, nullable = false)
    private String currency = "VND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private WalletTopupStatus status = WalletTopupStatus.SUCCEEDED;

    @Column(name = "checkout_url", columnDefinition = "text")
    private String checkoutUrl;

    @Column(name = "failure_code", length = 80)
    private String failureCode;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public WalletTopup() {}

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

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public WalletTopupStatus getStatus() {
        return status;
    }

    public void setStatus(WalletTopupStatus status) {
        this.status = status;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
