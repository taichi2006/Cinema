package com.cinema.wallet.dto.envelope;

/**
 * Chi tiết lỗi từng trường dữ liệu theo chuẩn Swagger.
 */
public record FieldError(
        String field,
        String message
) {}
