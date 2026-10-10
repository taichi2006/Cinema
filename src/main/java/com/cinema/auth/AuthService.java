package com.cinema.auth;

import com.cinema.common.exception.ApiException;
import com.cinema.user.entity.User;
import com.cinema.wallet.service.WalletService;
import org.mindrot.jbcrypt.BCrypt;

import com.cinema.auth.dto.request.ChangePasswordRequest;
import com.cinema.auth.dto.request.LoginRequest;
import com.cinema.auth.dto.request.RegisterRequest;
import com.cinema.auth.dto.request.RefreshRequest;
import com.cinema.auth.dto.response.AuthUserResponse;
import com.cinema.auth.dto.response.LoginResponse;
import com.cinema.auth.dto.response.RefreshResponse;
import io.jsonwebtoken.Claims;

import java.time.Instant;
import java.time.LocalDate;

public class AuthService {

    private final AuthDAO authDAO;

    public AuthService() {
        this(new AuthDAO());
    }

    public AuthService(AuthDAO authDAO) {
        this.authDAO = authDAO;
    }

    public AuthUserResponse register(RegisterRequest req) {
        if (req == null || req.email() == null || req.password() == null)
            throw ApiException.badRequest("Email và mật khẩu không được để trống");
        if (req.password().length() < 8)
            throw ApiException.badRequest("Mật khẩu phải có ít nhất 8 ký tự");

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
        String fullName = req.fullName() != null && !req.fullName().isBlank()
                ? req.fullName().trim()
                : email.split("@")[0];
        String phone = req.phone() != null && !req.phone().isBlank()
                ? req.phone().trim()
                : null;

        User user = authDAO.createUser(email, passwordHash, fullName, phone, dob);

        // Tự động khởi tạo ví rỗng cho user mới
        new WalletService().getOrCreateWallet(user.getId());

        return new AuthUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getStatus());
    }

    public LoginResponse login(LoginRequest req) {
        return login(req, null, null);
    }

    public LoginResponse login(LoginRequest req, String userAgent, String ipAddress) {
        if (req == null || req.email() == null || req.password() == null)
            throw ApiException.badRequest("Thiếu email hoặc mật khẩu");

        String email = req.email().trim().toLowerCase();

        User user = authDAO.findUserByEmail(email)
                .orElseThrow(AuthException::invalidCredentials);

        if (!BCrypt.checkpw(req.password(), user.getPasswordHash())) {
            throw AuthException.invalidCredentials();
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw AuthException.accountLocked();
        }

        String access = JwtUtil.generateAccessToken(user.getId());
        String refresh = JwtUtil.generateRefreshToken(user.getId());

        String tokenHash = JwtUtil.hashToken(refresh);
        Instant expiresAt = Instant.now().plusSeconds(JwtUtil.getRefreshTtlSeconds());
        authDAO.saveRefreshToken(user.getId(), tokenHash, expiresAt, userAgent, ipAddress);

        return new LoginResponse(
                access,
                refresh,
                "Bearer",
                JwtUtil.getAccessTtlSeconds(),
                new LoginResponse.UserInfo(user.getId(), user.getEmail())
        );
    }

    public RefreshResponse refresh(RefreshRequest req) {
        return refresh(req, null, null);
    }

    public RefreshResponse refresh(RefreshRequest req, String userAgent, String ipAddress) {
        if (req == null || req.refreshToken() == null || req.refreshToken().isBlank())
            throw ApiException.badRequest("Thiếu refresh token");

        try {
            Claims claims = JwtUtil.parseRefreshToken(req.refreshToken());
            long userId = Long.parseLong(claims.getSubject());

            String tokenHash = JwtUtil.hashToken(req.refreshToken());
            if (!authDAO.isRefreshTokenValid(tokenHash, userId)) {
                throw AuthException.refreshTokenExpired();
            }

            User user = authDAO.findUserById(userId)
                    .orElseThrow(AuthException::unauthorized);

            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw AuthException.accountLocked();
            }

            // Thu hồi token cũ (rotate refresh token)
            authDAO.revokeRefreshToken(tokenHash);

            // Tạo cặp token mới
            String newAccess = JwtUtil.generateAccessToken(user.getId());
            String newRefresh = JwtUtil.generateRefreshToken(user.getId());

            String newTokenHash = JwtUtil.hashToken(newRefresh);
            Instant newExpiresAt = Instant.now().plusSeconds(JwtUtil.getRefreshTtlSeconds());
            authDAO.saveRefreshToken(user.getId(), newTokenHash, newExpiresAt, userAgent, ipAddress);

            return new RefreshResponse(
                    newAccess,
                    newRefresh,
                    JwtUtil.getAccessTtlSeconds()
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw AuthException.refreshTokenExpired();
        }
    }

    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                String tokenHash = JwtUtil.hashToken(refreshToken);
                authDAO.revokeRefreshToken(tokenHash);
            } catch (Exception ignored) {
                // Đăng xuất an toàn kể cả khi token không hợp lệ
            }
        }
    }

    public void logout(long userId, String refreshToken) {
        logout(refreshToken);
        authDAO.revokeAllUserTokens(userId);
    }

    public void changePassword(long userId, ChangePasswordRequest req) {
        if (req == null || req.currentPassword() == null || req.newPassword() == null)
            throw ApiException.badRequest("Thiếu mật khẩu hiện tại hoặc mật khẩu mới");
        if (req.confirmPassword() != null && !req.confirmPassword().equals(req.newPassword()))
            throw ApiException.badRequest("Mật khẩu xác nhận không khớp");
        if (req.newPassword().length() < 8)
            throw ApiException.badRequest("Mật khẩu mới phải có ít nhất 8 ký tự");

        User user = authDAO.findUserById(userId)
                .orElseThrow(() -> ApiException.notFound("Người dùng không tồn tại"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw AuthException.accountLocked();
        }

        if (!BCrypt.checkpw(req.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(422, "Mật khẩu hiện tại không đúng");
        }

        String newPasswordHash = BCrypt.hashpw(req.newPassword(), BCrypt.gensalt(12));
        boolean success = authDAO.updatePasswordAndRevokeTokens(userId, user.getPasswordHash(), newPasswordHash);
        if (!success) {
            throw ApiException.internal("Không thể cập nhật mật khẩu");
        }
    }
}
