package com.cinema.wallet.dto.request;

import java.math.BigDecimal;

/**
 * Request body cho API nạp tiền (POST /wallet/top-up).
 */
public record TopUpRequest(
        BigDecimal amount,
        String method
) {}
