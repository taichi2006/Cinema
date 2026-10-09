package com.cinema.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;

public record ChangePasswordRequest(
        @JsonAlias({"oldPassword"}) String currentPassword,
        String newPassword,
        String confirmPassword
) {
    public String currentPassword() {
        return currentPassword;
    }

    public String oldPassword() {
        return currentPassword;
    }
}
