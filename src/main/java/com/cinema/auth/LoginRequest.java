package com.cinema.auth;

public record LoginRequest(
        String email,
        String password
) {
}
