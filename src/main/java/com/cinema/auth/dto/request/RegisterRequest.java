package com.cinema.auth.dto.request;

public record RegisterRequest(
        String fullName,
        String email,
        String phone,
        String password,
        String dob
) {}
