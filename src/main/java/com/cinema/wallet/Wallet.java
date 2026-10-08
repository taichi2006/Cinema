package com.cinema.wallet;

import com.cinema.user.entity.User;
import jakarta.persistence.*;

import java.time.Instant;

/**
 * Thực thể Ví người dùng
 * Quản lý số dư và trạng thái ví của từng tài khoản người dùng.
 */
@Entity
@Table(name = "wallets", schema = "cinema")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(nullable = false)
    private Long balance = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status = WalletStatus.ACTIVE;


    public Wallet() {}

    public Wallet(User user) {
        this.user = user;
        this.balance = 0L;
        this.status = WalletStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public void setStatus(WalletStatus status) {
        this.status = status;
    }

}

