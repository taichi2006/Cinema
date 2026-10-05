package com.cinema.auth;

public record ChangePasswordRequest(
        String oldPassword,
        String newPassword
) {
}
