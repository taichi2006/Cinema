package com.cinema.auth;

import com.cinema.common.exception.ApiException;

public class AuthException extends ApiException {
    
    public AuthException(String message) {
        super(401, message);
    }

    public AuthException(int status, String message) {
        super(status, message);
    }

    public static AuthException unauthorized() {
        return new AuthException("Chưa đăng nhập");
    }

    public static AuthException invalidCredentials() {
        return new AuthException("Email hoặc mật khẩu không đúng");
    }

    public static AuthException accountLocked() {
        return new AuthException(403, "Tài khoản đã bị khóa");
    }

    public static AuthException refreshTokenExpired() {
        return new AuthException("Refresh token đã hết hạn");
    }

    public static AuthException invalidToken() {
        return new AuthException("Refresh token đã hết hạn");
    }
}
