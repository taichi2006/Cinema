package com.cinema.auth;

public record AuthUserResponse(
        long userId,
        String email,
        String fullName,
        String role
) {
}
