package com.cinema.wallet.exception;

import com.cinema.common.exception.ApiException;

/**
 * Exception nghiệp vụ
 */
public class WalletException extends ApiException {

    private final String code;

    public WalletException(int status, String code, String message) {
        super(status, message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static WalletException idempotencyKeyRequired() {
        return new WalletException(400, "IDEMPOTENCY_KEY_REQUIRED", "Thiếu header Idempotency-Key (bắt buộc 16-128 ký tự)");
    }

    public static WalletException walletSuspended() {
        return new WalletException(403, "WALLET_SUSPENDED", "Ví của bạn đang bị tạm khóa (SUSPENDED), không thể thực hiện giao dịch");
    }

    public static WalletException invalidAmount(String message) {
        return new WalletException(422, "TOP_UP_AMOUNT_INVALID", message);
    }

    public static WalletException resourceNotFound(String message) {
        return new WalletException(404, "RESOURCE_NOT_FOUND", message);
    }

    public static WalletException invalidFilter(String message, String field) {
        return new WalletException(400, "INVALID_FILTER", message);
    }

    public static WalletException invalidFilter(String message) {
        return new WalletException(400, "INVALID_FILTER", message);
    }

    public static WalletException forbidden(String message) {
        return new WalletException(403, "FORBIDDEN", message);
    }
}

