package com.cinema.wallet;

/**
 * Trạng thái của giao dịch ví (Đồng bộ với swagger_cinema.yaml và UML):
 * - PENDING: Chờ xử lý / chờ admin duyệt nạp tiền
 * - SUCCEEDED: Giao dịch thành công (chuẩn Swagger)
 * - SUCCESSFUL: Giao dịch thành công (chuẩn UML)
 * - FAILED: Giao dịch thất bại / bị từ chối
 * - EXPIRED: Giao dịch hết hạn thanh toán
 * - REVERSAL_PENDING, REVERSED: Đảo giao dịch / hoàn tiền
 */
public enum TransactionStatus {
    PENDING,
    SUCCEEDED,
    SUCCESSFUL,
    FAILED,
    EXPIRED,
    REVERSAL_PENDING,
    REVERSED;

    /**
     * Kiểm tra trạng thái giao dịch có phải là thành công hay không.
     */
    public boolean isSuccessful() {
        return this == SUCCEEDED || this == SUCCESSFUL || this == PENDING;
    }
}
