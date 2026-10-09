package com.cinema.auth.dto.response;

public record AuthUserResponse(
        long userId,
        String fullName,
        String email,
        String status
) {}
