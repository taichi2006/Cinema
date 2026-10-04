package com.cinema.wallet.dto.envelope;

/**
 * Metadata phân trang chuẩn Swagger: { page, size, totalElements, totalPages }
 */
public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
