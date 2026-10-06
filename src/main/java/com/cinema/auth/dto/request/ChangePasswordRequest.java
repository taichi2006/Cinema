package com.cinema.auth.dto.request;

public record ChangePasswordRequest(String oldPassword, String newPassword) {}
