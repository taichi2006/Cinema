package com.cinema.user.dto.response;

public record UserProfileResponse(
        long userId,
        String fullName,
        String email,
        String phone,
        String status
) {
}
