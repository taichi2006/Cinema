package com.cinema.wallet.dto.response;

import java.math.BigDecimal;

/**
 * Response schema cho kết quả tạo/theo dõi yêu cầu nạp tiền (POST /wallet/top-up, GET /wallet/top-up/{id}).
 */
public record TopUpResponse(
        String id,
        BigDecimal amount,
        String currency,
        String method,
        String status,
        String checkoutUrl,
        String expiresAt,
        String createdAt,
        String completedAt,
        String failureCode
) {}
