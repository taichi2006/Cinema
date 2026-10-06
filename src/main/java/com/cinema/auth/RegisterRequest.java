package com.cinema.auth;

public record RegisterRequest(
        String email,
        String password,
        String fullName
) {
}
