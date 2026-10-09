package com.cinema.auth.dto.response;

public record RefreshResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
