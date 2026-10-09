package com.cinema.user.dto.response;

public record UserBookingResponse(
        long bookingId,
        String status,
        long totalAmount
) {
}
