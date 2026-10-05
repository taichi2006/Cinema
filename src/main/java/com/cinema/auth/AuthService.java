package com.cinema.auth;

import com.cinema.common.exception.ApiException;
import com.cinema.user.entity.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Objects;

public class AuthService {

    private static final String USER_ROLE = "USER";
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final AuthDAO authDAO;

    public AuthService() {
        this(new AuthDAO());
    }

    AuthService(AuthDAO authDAO) {
        this.authDAO = Objects.requireNonNull(authDAO, "AuthDAO must not be null");
    }

    public AuthUserResponse register(RegisterRequest request) {
        validateRegisterRequest(request);
        String email = normalizeEmail(request.email());

        try {
            if (authDAO.existsByEmail(email)) {
                throw ApiException.conflict("Email đã được sử dụng");
            }

            long roleId = authDAO.findRoleIdByName(USER_ROLE)
                    .orElseThrow(() -> ApiException.internal("Lỗi server: Chưa tạo role USER"));
            String fullName = request.fullName() == null
                    ? email.split("@")[0]
                    : request.fullName().trim();

            User user = authDAO.createUser(
                    email,
                    BCrypt.hashpw(request.password(), BCrypt.gensalt(12)),
                    fullName,
                    roleId
            );
            return new AuthUserResponse(user.getId(), user.getEmail(), user.getFullName(), USER_ROLE);
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApiException.internal("Không thể đăng ký tài khoản");
        }
    }

    public LoginResponse login(LoginRequest request) {
        validateLoginRequest(request);
        String email = normalizeEmail(request.email());

        try {
            User user = authDAO.findUserWithRoleByEmail(email)
                    .orElseThrow(AuthException::invalidCredentials);
            ensureActive(user);

            if (!BCrypt.checkpw(request.password(), user.getPasswordHash())) {
                throw AuthException.invalidCredentials();
            }

            String roleName = user.getRole().getName();
            String accessToken = JwtUtil.generateAccessToken(
                    user.getId(),
                    roleName,
                    user.getAuthVersion()
            );

            return new LoginResponse(
                    new AuthUserResponse(
                            user.getId(), user.getEmail(), user.getFullName(), roleName
                    ),
                    accessToken
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApiException.internal("Không thể đăng nhập");
        }
    }

    public void validateAccessIdentity(long userId, String role, int authVersion) {
        try {
            User user = authDAO.findUserWithRoleById(userId)
                    .orElseThrow(AuthException::invalidToken);
            ensureActive(user);

            if (!user.getRole().getName().equals(role)
                    || user.getAuthVersion() == null
                    || user.getAuthVersion() != authVersion) {
                throw AuthException.invalidToken();
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApiException.internal("Không thể xác thực phiên đăng nhập");
        }
    }

    public void changePassword(long userId, ChangePasswordRequest request) {
        validateChangePasswordRequest(request);

        try {
            User user = authDAO.findUserWithRoleById(userId)
                    .orElseThrow(AuthException::invalidToken);
            ensureActive(user);

            if (!BCrypt.checkpw(request.oldPassword(), user.getPasswordHash())) {
                throw ApiException.badRequest("Mật khẩu cũ không đúng");
            }

            String newPasswordHash = BCrypt.hashpw(request.newPassword(), BCrypt.gensalt(12));
            boolean updated = authDAO.updatePasswordAndRevokeTokens(
                    userId,
                    user.getPasswordHash(),
                    newPasswordHash
            );
            if (!updated) {
                throw AuthException.invalidToken();
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw ApiException.internal("Không thể đổi mật khẩu");
        }
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            throw ApiException.badRequest("Email và mật khẩu không được để trống");
        }
        if (request.password().length() < MIN_PASSWORD_LENGTH) {
            throw ApiException.badRequest("Mật khẩu phải có ít nhất 8 ký tự");
        }
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            throw ApiException.badRequest("Thiếu email hoặc mật khẩu");
        }
    }

    private void validateChangePasswordRequest(ChangePasswordRequest request) {
        if (request == null || request.oldPassword() == null || request.newPassword() == null) {
            throw ApiException.badRequest("Thiếu oldPassword hoặc newPassword");
        }
        if (request.newPassword().length() < MIN_PASSWORD_LENGTH) {
            throw ApiException.badRequest("Mật khẩu mới phải có ít nhất 8 ký tự");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private void ensureActive(User user) {
        if (!ACTIVE_STATUS.equals(user.getStatus())) {
            throw AuthException.accountLocked();
        }
    }
}
