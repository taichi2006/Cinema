package com.cinema.wallet.dto.response;

import java.math.BigDecimal;

/**
 * Response schema cho từng mục bút toán lịch sử giao dịch ví (GET /wallet/transaction).
 */
public record TransactionItemResponse(
        String id,
        String type,
        String direction,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String currency,
        String referenceType,
        String referenceId,
        String description,
        String createdAt
) {}
