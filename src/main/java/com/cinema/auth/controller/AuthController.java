package com.cinema.auth.controller;

import com.cinema.auth.dto.request.ChangePasswordRequest;
import com.cinema.auth.dto.request.LoginRequest;
import com.cinema.auth.dto.request.RefreshRequest;
import com.cinema.auth.dto.request.RegisterRequest;
import com.cinema.auth.dto.response.LoginResponse;
import com.cinema.auth.dto.response.RefreshResponse;
import com.cinema.auth.service.AuthService;
import com.cinema.common.util.JwtUtil;
import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/auth", "/auth/*"})
public class AuthController extends BaseServlet {

    private final AuthService service = new AuthService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] segments = getPathSegments(req);

        // POST /auth/{action}
        if (segments.length == 1) {
            String action = segments[0].toLowerCase();

            // 1. POST /auth/register (Đăng ký tài khoản)
            if ("register".equals(action)) {
                RegisterRequest body = parseBody(req, RegisterRequest.class);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                writeSuccess(resp, service.register(body));
                return;
            }

            // 2. POST /auth/login (Đăng nhập)
            if ("login".equals(action)) {
                LoginRequest body = parseBody(req, LoginRequest.class);
                LoginResponse result = service.login(body);
                resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_ACCESS, result.accessToken(), (int) result.expiresIn()));
                resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_REFRESH, result.refreshToken(), JwtUtil.getRefreshTtlSeconds()));
                writeSuccess(resp, result);
                return;
            }

            // 3. POST /auth/logout (Đăng xuất)
            if ("logout".equals(action)) {
                Long userId = getOptionalUserId(req);
                String refreshToken = resolveRefreshToken(req);
                if (userId != null) {
                    service.logout(userId, refreshToken);
                } else {
                    service.logout(refreshToken);
                }
                resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_ACCESS, "", 0));
                resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_REFRESH, "", 0));
                writeSuccess(resp, null);
                return;
            }

            // 4. POST /auth/refresh (Làm mới token)
            if ("refresh".equals(action)) {
                String refreshToken = resolveRefreshToken(req);
                if (refreshToken == null || refreshToken.isBlank()) {
                    throw ApiException.badRequest("Thiếu refresh token");
                }
                RefreshResponse result = service.refresh(new RefreshRequest(refreshToken));
                resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_ACCESS, result.accessToken(), (int) result.expiresIn()));
                resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_REFRESH, result.refreshToken(), JwtUtil.getRefreshTtlSeconds()));
                writeSuccess(resp, result);
                return;
            }

            // 5. POST /auth/change-password (Đổi mật khẩu)
            if ("change-password".equals(action)) {
                long userId = getAuthenticatedUserId(req);
                ChangePasswordRequest body = parseBody(req, ChangePasswordRequest.class);
                service.changePassword(userId, body);
                writeSuccess(resp, null);
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại: " + action);
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }

    private String resolveRefreshToken(HttpServletRequest req) {
        String token = JwtUtil.readCookie(req, JwtUtil.COOKIE_REFRESH);
        if (token != null && !token.isBlank()) {
            return token.trim();
        }
        RefreshRequest body = parseOptionalBody(req, RefreshRequest.class);
        return (body != null && body.refreshToken() != null) ? body.refreshToken().trim() : null;
    }
}
