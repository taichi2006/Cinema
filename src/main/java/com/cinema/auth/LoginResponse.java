package com.cinema.auth;

public record LoginResponse(
        AuthUserResponse user,
        String accessToken
) {
}
