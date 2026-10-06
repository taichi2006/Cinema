package com.cinema.wallet;

/**
 * Trạng thái yêu cầu nạp tiền:
 * - PENDING          : Chờ xử lý
 * - SUCCEEDED        : Nạp tiền thành công
 * - FAILED           : Nạp tiền thất bại
 * - EXPIRED          : Yêu cầu hết hạn
 * - REVERSAL_PENDING : Chờ đảo giao dịch
 * - REVERSED         : Đã hoàn tất đảo giao dịch
 */
public enum WalletTopupStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    EXPIRED,
    REVERSAL_PENDING,
    REVERSED;

    public boolean isSuccessful() {
        return this == SUCCEEDED;
    }
}
