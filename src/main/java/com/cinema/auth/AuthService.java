package com.cinema.auth;

import com.cinema.auth.dto.request.ChangePasswordRequest;
import com.cinema.auth.dto.request.LoginRequest;
import com.cinema.auth.dto.request.RefreshRequest;
import com.cinema.auth.dto.request.RegisterRequest;
import com.cinema.auth.dto.response.AuthUserResponse;
import com.cinema.auth.dto.response.LoginResponse;
import com.cinema.auth.dto.response.RefreshResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.user.entity.User;
import com.cinema.wallet.service.WalletService;
import io.jsonwebtoken.Claims;
import org.mindrot.jbcrypt.BCrypt;

import java.time.Instant;
import java.time.LocalDate;

public class AuthService {

    private final AuthDAO authDAO = new AuthDAO();

    // 1. Đăng ký tài khoản
    public AuthUserResponse register(RegisterRequest req) {
        if (req == null || req.email() == null || req.email().isBlank() || req.password() == null || req.password().isBlank()) {
            throw ApiException.badRequest("Email và mật khẩu không được để trống");
        }
        if (req.password().trim().length() < 8) {
            throw ApiException.badRequest("Mật khẩu phải có ít nhất 8 ký tự");
        }

        String email = req.email().trim().toLowerCase();
        if (authDAO.existsByEmail(email)) {
            throw ApiException.conflict("Email đã được đăng ký");
        }

        LocalDate dob = null;
        if (req.dob() != null && !req.dob().isBlank()) {
            try {
                dob = LocalDate.parse(req.dob().trim());
            } catch (Exception e) {
                throw ApiException.badRequest("Định dạng ngày sinh không hợp lệ (yyyy-MM-dd)");
            }
        }

        String passwordHash = BCrypt.hashpw(req.password(), BCrypt.gensalt(12));
        String fullName = (req.fullName() != null && !req.fullName().isBlank()) ? req.fullName().trim() : email.split("@")[0];
        String phone = (req.phone() != null && !req.phone().isBlank()) ? req.phone().trim() : null;

        User user = authDAO.createUser(email, passwordHash, fullName, phone, dob);
        new WalletService().getOrCreateWallet(user.getId());

        return new AuthUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getStatus());
    }

    // 2. Đăng nhập
    public LoginResponse login(LoginRequest req) {
        if (req == null || req.email() == null || req.email().isBlank() || req.password() == null || req.password().isBlank()) {
            throw ApiException.badRequest("Email và mật khẩu không được để trống");
        }

        String email = req.email().trim().toLowerCase();
        User user = authDAO.findUserByEmail(email)
                .orElseThrow(() -> ApiException.unauthorized("Email hoặc mật khẩu không đúng"));

        if (user.getPasswordHash() == null || !BCrypt.checkpw(req.password(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Email hoặc mật khẩu không đúng");
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw ApiException.forbidden("Tài khoản đã bị khóa");
        }

        // Thu hồi toàn bộ refresh token cũ của user trên các thiết bị trước đó
        authDAO.revokeAllUserTokens(user.getId());

        // Tăng phiên bản token JWT: Vô hiệu hóa toàn bộ Access Token của các thiết bị đăng nhập trước đó
        int tokenVersion = authDAO.incrementAndGetTokenVersion(user.getId());

        String access = JwtUtil.generateAccessToken(user.getId(), tokenVersion);
        String refresh = JwtUtil.generateRefreshToken(user.getId(), tokenVersion);
        Instant expiresAt = Instant.now().plusSeconds(JwtUtil.getRefreshTtlSeconds());
        authDAO.saveRefreshToken(user.getId(), refresh, expiresAt);

        return new LoginResponse(
                access, refresh, "Bearer", JwtUtil.getAccessTtlSeconds(),
                new LoginResponse.UserInfo(user.getId(), user.getEmail())
        );
    }

    // 3. Làm mới Access Token (Refresh)
    public RefreshResponse refresh(RefreshRequest req) {
        if (req == null || req.refreshToken() == null || req.refreshToken().isBlank()) {
            throw ApiException.badRequest("Thiếu refresh token");
        }

        try {
            Claims claims = JwtUtil.parseRefreshToken(req.refreshToken());
            long userId = Long.parseLong(claims.getSubject());
            Integer tokenVer = claims.get("token_version", Integer.class);

            if (tokenVer == null || !authDAO.isTokenVersionValid(userId, tokenVer)) {
                throw ApiException.unauthorized("Tài khoản đã được đăng nhập từ một thiết bị khác");
            }

            if (!authDAO.isRefreshTokenValid(req.refreshToken(), userId)) {
                throw ApiException.unauthorized("Refresh token đã hết hạn");
            }

            User user = authDAO.findUserById(userId)
                    .orElseThrow(() -> ApiException.unauthorized("Chưa đăng nhập"));
            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw ApiException.forbidden("Tài khoản đã bị khóa");
            }

            String newAccess = JwtUtil.generateAccessToken(user.getId(), tokenVer);
            return new RefreshResponse(newAccess, req.refreshToken(), JwtUtil.getAccessTtlSeconds());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.unauthorized("Refresh token không hợp lệ hoặc đã hết hạn");
        }
    }

    // 4. Đăng xuất
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                Claims claims = JwtUtil.parseRefreshToken(refreshToken);
                long userId = Long.parseLong(claims.getSubject());
                authDAO.invalidateTokenVersion(userId);
            } catch (Exception ignored) {}
            authDAO.revokeRefreshToken(refreshToken);
        }
    }

    public void logout(long userId, String refreshToken) {
        logout(refreshToken);
        authDAO.invalidateTokenVersion(userId);
        authDAO.revokeAllUserTokens(userId);
    }

    // 5. Đổi mật khẩu
    public void changePassword(long userId, ChangePasswordRequest req) {
        if (req == null || req.currentPassword() == null || req.currentPassword().isBlank()
                || req.newPassword() == null || req.newPassword().isBlank()) {
            throw ApiException.badRequest("Mật khẩu hiện tại và mật khẩu mới không được để trống");
        }
        if (req.newPassword().trim().length() < 8) {
            throw ApiException.badRequest("Mật khẩu mới phải có ít nhất 8 ký tự");
        }
        if (req.confirmPassword() != null && !req.confirmPassword().equals(req.newPassword())) {
            throw ApiException.badRequest("Mật khẩu xác nhận không khớp");
        }

        User user = authDAO.findUserById(userId)
                .orElseThrow(() -> ApiException.notFound("Người dùng không tồn tại"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw ApiException.forbidden("Tài khoản đã bị khóa");
        }
        if (user.getPasswordHash() == null || !BCrypt.checkpw(req.currentPassword(), user.getPasswordHash())) {
            throw ApiException.unprocessable("Mật khẩu hiện tại không đúng");
        }

        String newPasswordHash = BCrypt.hashpw(req.newPassword(), BCrypt.gensalt(12));
        if (!authDAO.updatePasswordAndRevokeTokens(userId, user.getPasswordHash(), newPasswordHash)) {
            throw ApiException.internal("Không thể cập nhật mật khẩu");
        }
        authDAO.invalidateTokenVersion(userId);
    }
}
