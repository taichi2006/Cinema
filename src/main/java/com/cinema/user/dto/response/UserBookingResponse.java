package com.cinema.user.dto.response;

import java.time.Instant;
import java.util.List;

public record UserBookingResponse(
        String id,
        String status,
        String movieTitle,
        String cinemaName,
        String roomName,
        Instant startsAt,
        List<String> seatLabels,
        long totalAmount,
        String currency,
        Instant createdAt
) {
}
