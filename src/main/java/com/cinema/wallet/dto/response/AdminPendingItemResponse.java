package com.cinema.wallet.dto.response;

import java.math.BigDecimal;

/**
 * Response schema cho danh sách yêu cầu nạp tiền chờ duyệt của Admin (GET /wallet/top-up/pending).
 */
public record AdminPendingItemResponse(
        String id,
        Long walletId,
        Long userId,
        String userEmail,
        BigDecimal amount,
        String currency,
        String method,
        String status,
        String description,
        String createdAt,
        String expiresAt
) {}
