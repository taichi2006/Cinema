package com.cinema.wallet;

import com.cinema.common.exception.ApiException;
import com.cinema.wallet.dto.envelope.FieldError;

import java.util.List;

/**
 * Exception nghiệp vụ dành riêng cho Module Wallet.
 * Mang theo mã lỗi (error code) và danh sách fieldErrors theo chuẩn swagger_cinema.yaml.
 */
public class WalletException extends ApiException {

    private final String code;
    private final List<FieldError> fieldErrors;

    public WalletException(int status, String code, String message, List<FieldError> fieldErrors) {
        super(status, message);
        this.code = code;
        this.fieldErrors = fieldErrors;
    }

    public WalletException(int status, String code, String message) {
        this(status, code, message, null);
    }

    public String getCode() {
        return code;
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }

    public static WalletException idempotencyKeyRequired() {
        return new WalletException(400, "IDEMPOTENCY_KEY_REQUIRED", "Thiếu header Idempotency-Key",
                List.of(new FieldError("idempotencyKey", "Header idempotencyKey là bắt buộc (16-128 ký tự)")));
    }

    public static WalletException walletSuspended() {
        return new WalletException(403, "WALLET_SUSPENDED", "Ví của bạn đang bị tạm khóa (SUSPENDED), không thể thực hiện giao dịch");
    }

    public static WalletException invalidAmount(String message) {
        return new WalletException(422, "TOP_UP_AMOUNT_INVALID", message,
                List.of(new FieldError("amount", message)));
    }

    public static WalletException resourceNotFound(String message) {
        return new WalletException(404, "RESOURCE_NOT_FOUND", message);
    }

    public static WalletException invalidFilter(String message, String field) {
        return new WalletException(400, "INVALID_FILTER", message,
                field != null ? List.of(new FieldError(field, message)) : null);
    }

    public static WalletException forbidden(String message) {
        return new WalletException(403, "FORBIDDEN", message);
    }
}
