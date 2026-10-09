package com.cinema.user.dto.response;

public record UserVoucherResponse(
        long voucherId,
        String code,
        String status
) {
}
