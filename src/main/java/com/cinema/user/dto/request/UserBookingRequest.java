package com.cinema.user.dto.request;

public record UserBookingRequest(
        String status,
        String from,
        String to,
        String page,
        String size,
        String sort
) {
}
