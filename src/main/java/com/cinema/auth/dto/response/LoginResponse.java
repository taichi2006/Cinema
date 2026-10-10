package com.cinema.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserInfo user
) {
    public record UserInfo(long userId, String email) {}
}
