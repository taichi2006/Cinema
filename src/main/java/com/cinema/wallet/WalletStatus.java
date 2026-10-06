package com.cinema.wallet;

/**
 * Trạng thái hoạt động của ví:
 * - ACTIVE: Ví hoạt động bình thường, cho phép nạp tiền và thanh toán.
 * - SUSPENDED: Ví bị tạm khóa, chặn mọi giao dịch phát sinh tiền (nạp/thanh toán).
 */
public enum WalletStatus {
    ACTIVE,
    SUSPENDED
}
