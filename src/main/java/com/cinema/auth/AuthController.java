package com.cinema.auth;

import com.cinema.auth.dto.request.ChangePasswordRequest;
import com.cinema.auth.dto.request.LoginRequest;
import com.cinema.auth.dto.request.RefreshRequest;
import com.cinema.auth.dto.request.RegisterRequest;
import com.cinema.auth.dto.response.LoginResponse;
import com.cinema.auth.dto.response.RefreshResponse;
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
            switch (action) {
                case "register"        -> handleRegister(req, resp);
                case "login"           -> handleLogin(req, resp);
                case "logout"          -> handleLogout(req, resp);
                case "refresh"         -> handleRefresh(req, resp);
                case "change-password" -> handleChangePassword(req, resp);
                default                -> throw ApiException.notFound("Endpoint không tồn tại: " + action);
            }
            return;
        }

        throw ApiException.notFound("Đường dẫn không hợp lệ: " + req.getRequestURI());
    }

    private void handleRegister(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RegisterRequest body = parseBody(req, RegisterRequest.class);
        resp.setStatus(HttpServletResponse.SC_CREATED);
        writeSuccess(resp, service.register(body));
    }

    private void handleLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LoginRequest body = parseBody(req, LoginRequest.class);
        LoginResponse result = service.login(body);
        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_ACCESS, result.accessToken(), (int) result.expiresIn()));
        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_REFRESH, result.refreshToken(), JwtUtil.getRefreshTtlSeconds()));
        writeSuccess(resp, result);
    }

    private void handleLogout(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
    }

    private void handleRefresh(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String refreshToken = resolveRefreshToken(req);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw ApiException.badRequest("Thiếu refresh token");
        }

        RefreshResponse result = service.refresh(new RefreshRequest(refreshToken));
        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_ACCESS, result.accessToken(), (int) result.expiresIn()));
        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_REFRESH, result.refreshToken(), JwtUtil.getRefreshTtlSeconds()));
        writeSuccess(resp, result);
    }

    private void handleChangePassword(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        service.changePassword(userId, parseBody(req, ChangePasswordRequest.class));
        writeSuccess(resp, null);
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
