package com.cinema.wallet;

/**
 * Loại giao dịch của ví:
 * - TOP_UP: Nạp tiền vào ví
 * - PAYMENT: Thanh toán (vé, bắp nước, ...)
 * - REFUND: Hoàn tiền vào ví
 */
public enum TransactionType {
    TOP_UP,
    PAYMENT,
    REFUND
}
