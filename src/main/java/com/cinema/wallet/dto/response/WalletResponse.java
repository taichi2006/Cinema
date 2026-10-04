package com.cinema.wallet.dto.response;

import java.math.BigDecimal;

/**
 * Response schema cho thông tin số dư ví (GET /wallet).
 */
public record WalletResponse(
        String id,
        BigDecimal balance,
        String currency,
        String updatedAt
) {}
