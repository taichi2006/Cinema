package com.cinema.auth;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.common.exception.ErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

import com.cinema.auth.dto.request.ChangePasswordRequest;
import com.cinema.auth.dto.request.LoginRequest;
import com.cinema.auth.dto.request.RegisterRequest;
import com.cinema.auth.dto.request.RefreshRequest;
import com.cinema.auth.dto.response.AuthUserResponse;
import com.cinema.auth.dto.response.LoginResponse;
import com.cinema.auth.dto.response.RefreshResponse;

@WebServlet(urlPatterns = {
        "/auth", "/auth/*",
        "/api/auth", "/api/auth/*",
        "/api/v1/auth", "/api/v1/auth/*"
})
public class AuthController extends HttpServlet {

    private final AuthService  service = new AuthService();
    private final ObjectMapper json    = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String action = req.getPathInfo();
        if (action == null || action.isBlank() || "/".equals(action)) {
            String fullPath = req.getRequestURI().substring(req.getContextPath().length());
            action = fullPath.replaceAll("^/(api/v1|api)?/auth", "");
        }
        if (action.endsWith("/") && action.length() > 1) {
            action = action.substring(0, action.length() - 1);
        }

        try {
            switch (action) {
                case "/register"        -> doRegister(req, resp);
                case "/login"           -> doLogin(req, resp);
                case "/logout"          -> doLogout(req, resp);
                case "/refresh"         -> doRefresh(req, resp);
                case "/change-password" -> doChangePassword(req, resp);
                default -> throw ApiException.notFound("Endpoint không tồn tại");
            }
        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void doRegister(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        var body = json.readValue(req.getInputStream(), RegisterRequest.class);
        AuthUserResponse user = service.register(body);
        resp.setStatus(HttpServletResponse.SC_CREATED); // 201
        write(resp, ApiResponse.ok(user));
    }

    private void doLogin(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        var body = json.readValue(req.getInputStream(), LoginRequest.class);
        String userAgent = req.getHeader("User-Agent");
        String ipAddress = getClientIp(req);

        LoginResponse result = service.login(body, userAgent, ipAddress);

        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_ACCESS, result.accessToken(), (int) result.expiresIn()));
        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_REFRESH, result.refreshToken(), JwtUtil.getRefreshTtlSeconds()));
        write(resp, ApiResponse.ok(result));
    }

    private void doLogout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long userId = getAuthenticatedUserId(req);
        if (userId == null) {
            throw AuthException.unauthorized();
        }

        String refreshToken = JwtUtil.readCookie(req, JwtUtil.COOKIE_REFRESH);
        if ((refreshToken == null || refreshToken.isBlank()) && req.getContentLengthLong() > 0) {
            try {
                var body = json.readValue(req.getInputStream(), RefreshRequest.class);
                refreshToken = body.refreshToken();
            } catch (Exception ignored) {}
        }

        service.logout(userId, refreshToken);

        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_ACCESS, "", 0));
        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_REFRESH, "", 0));
        write(resp, ApiResponse.ok(null));
    }

    private void doRefresh(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String refreshToken = null;
        if (req.getContentLengthLong() > 0) {
            try {
                var body = json.readValue(req.getInputStream(), RefreshRequest.class);
                if (body != null) {
                    refreshToken = body.refreshToken();
                }
            } catch (Exception ignored) {}
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            refreshToken = JwtUtil.readCookie(req, JwtUtil.COOKIE_REFRESH);
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            throw ApiException.badRequest("Thiếu refresh token");
        }

        String userAgent = req.getHeader("User-Agent");
        String ipAddress = getClientIp(req);

        RefreshResponse result = service.refresh(new RefreshRequest(refreshToken), userAgent, ipAddress);

        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_ACCESS, result.accessToken(), (int) result.expiresIn()));
        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_REFRESH, result.refreshToken(), JwtUtil.getRefreshTtlSeconds()));
        write(resp, ApiResponse.ok(result));
    }

    private void doChangePassword(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Long userId = getAuthenticatedUserId(req);
        if (userId == null) {
            throw AuthException.unauthorized();
        }

        var body = json.readValue(req.getInputStream(), ChangePasswordRequest.class);
        service.changePassword(userId, body);
        write(resp, ApiResponse.ok(null));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Long getAuthenticatedUserId(HttpServletRequest req) {
        Object userIdAttr = req.getAttribute("userId");
        if (userIdAttr instanceof Number num) {
            return num.longValue();
        }

        String token = null;
        String authHeader = req.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token == null || token.isBlank()) {
            token = JwtUtil.readCookie(req, JwtUtil.COOKIE_ACCESS);
        }
        if (token != null && !token.isBlank()) {
            try {
                Claims claims = JwtUtil.parseAccessToken(token);
                return Long.parseLong(claims.getSubject());
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String getClientIp(HttpServletRequest req) {
        String header = req.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            return header.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private void write(HttpServletResponse resp, Object body) throws IOException {
        json.writeValue(resp.getWriter(), body);
    }
}
