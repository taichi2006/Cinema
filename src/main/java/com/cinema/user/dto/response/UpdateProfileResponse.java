package com.cinema.user.dto.response;

public record UpdateProfileResponse(
        long userId,
        String fullName
) {
}
