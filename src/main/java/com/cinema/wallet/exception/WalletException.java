package com.cinema.wallet.exception;

import com.cinema.common.exception.ApiException;

/**
 * Exception nghiệp vụ cho Module Wallet
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

    public static WalletException walletSuspended() {
        return new WalletException(403, "WALLET_SUSPENDED", "Ví của bạn đang bị tạm khóa (SUSPENDED)");
    }

    public static WalletException invalidAmount(String message) {
        return new WalletException(422, "INVALID_AMOUNT", message);
    }

    public static WalletException invalidMethod(String message) {
        return new WalletException(400, "INVALID_METHOD", message);
    }

    public static WalletException invalidFilter(String message) {
        return new WalletException(400, "INVALID_FILTER", message);
    }

    public static WalletException resourceNotFound(String message) {
        return new WalletException(404, "RESOURCE_NOT_FOUND", message);
    }
}
