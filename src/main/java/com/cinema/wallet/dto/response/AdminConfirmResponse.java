package com.cinema.wallet.dto.response;

import java.math.BigDecimal;

/**
 * Response schema sau khi Admin duyệt nạp tiền (POST /wallet/top-up/{id}/confirm).
 */
public record AdminConfirmResponse(
        String id,
        Long walletId,
        BigDecimal amount,
        String currency,
        String status,
        BigDecimal newBalance,
        String completedAt
) {}
