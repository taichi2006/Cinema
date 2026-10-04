package com.cinema.auth;

import com.cinema.common.exception.ApiException;

public class AuthException extends ApiException {
    
    public AuthException(String message) {
        super(401, message);
    }

    public static AuthException unauthorized() {
        return new AuthException("Chưa đăng nhập hoặc phiên làm việc đã hết hạn");
    }

    public static AuthException invalidCredentials() {
        return new AuthException("Email hoặc mật khẩu không đúng");
    }

    public static AuthException accountLocked() {
        return new AuthException("Tài khoản đã bị khóa");
    }

    public static AuthException invalidToken() {
        return new AuthException("Token không hợp lệ hoặc đã hết hạn");
    }
}
