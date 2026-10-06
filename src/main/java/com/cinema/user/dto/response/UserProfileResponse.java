package com.cinema.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDate;

public record UserProfileResponse(
        String id,
        String fullName,
        String email,
        String phone,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateOfBirth,
        String role,
        Instant createdAt
) {
}
