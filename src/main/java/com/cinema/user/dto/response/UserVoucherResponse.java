package com.cinema.user.dto.response;

import java.time.Instant;
import java.util.List;

public record UserVoucherResponse(
        String id,
        String code,
        String description,
        String status,
        String discountType,
        long discountValue,
        Long maxDiscountAmount,
        long minOrderAmount,
        String appliesTo,
        List<String> eligibleCinemaIds,
        List<String> eligibleMovieIds,
        int remainingUses,
        Instant startsAt,
        Instant expiresAt,
        String currency
) {
}
