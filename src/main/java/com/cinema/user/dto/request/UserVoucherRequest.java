package com.cinema.user.dto.request;

public record UserVoucherRequest(
        String status,
        String page,
        String size
) {
}
