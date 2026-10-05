package com.cinema.auth.dto.response;

public record LoginResponse(AuthUserResponse user, String accessToken, String refreshToken) {}
