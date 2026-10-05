package com.cinema.auth.dto.response;

public record AuthUserResponse(long userId, String email, String fullName, String role) {}
