package com.cinema.wallet.dto.request;

/**
 * Request body cho API Admin duyệt nạp tiền (POST /wallet/top-up/{id}/confirm).
 */
public record AdminConfirmRequest(
        Boolean approve,
        String note
) {}
