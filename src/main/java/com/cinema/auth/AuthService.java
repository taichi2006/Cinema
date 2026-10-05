package com.cinema.auth;

import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.user.Role;
import com.cinema.user.User;

import jakarta.persistence.EntityManager;
import org.mindrot.jbcrypt.BCrypt;

import com.cinema.auth.dto.request.LoginRequest;
import com.cinema.auth.dto.request.RegisterRequest;
import com.cinema.auth.dto.request.RefreshRequest;
import com.cinema.auth.dto.response.AuthUserResponse;
import com.cinema.auth.dto.response.LoginResponse;
import io.jsonwebtoken.Claims;

public class AuthService {

    public AuthUserResponse register(RegisterRequest req) {
        if (req.email() == null || req.password() == null)
            throw ApiException.badRequest("Email và mật khẩu không được để trống");
        if (req.password().length() < 8)
            throw ApiException.badRequest("Mật khẩu phải có ít nhất 8 ký tự");

        String email = req.email().trim().toLowerCase();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery("SELECT count(u) FROM User u WHERE u.email = :email", Long.class)
                    .setParameter("email", email)
                    .getSingleResult();
            if (count > 0) throw ApiException.conflict("Email đã được sử dụng");

            Role userRole = em.createQuery("SELECT r FROM Role r WHERE r.name = 'USER'", Role.class)
                    .getResultStream().findFirst()
                    .orElseThrow(() -> ApiException.internal("Lỗi server: Chưa tạo role USER"));

            User u = new User();
            u.setEmail(email);
            u.setPasswordHash(BCrypt.hashpw(req.password(), BCrypt.gensalt(12)));
            u.setFullName(req.fullName() != null ? req.fullName().trim() : email.split("@")[0]);
            u.setRole(userRole);

            em.getTransaction().begin();
            em.persist(u);
            em.getTransaction().commit();

            return new AuthUserResponse(u.getId(), u.getEmail(), u.getFullName(), "USER");
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            if (e instanceof ApiException) throw e;
            throw ApiException.internal("Lỗi hệ thống: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public LoginResponse login(LoginRequest req) {
        if (req.email() == null || req.password() == null)
            throw ApiException.badRequest("Thiếu email hoặc mật khẩu");

        String email = req.email().trim().toLowerCase();

        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.createQuery("SELECT u FROM User u JOIN FETCH u.role WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getResultStream().findFirst()
                    .orElseThrow(AuthException::invalidCredentials);

            if ("DISABLED".equals(user.getStatus()))
                throw AuthException.accountLocked();

            if (!BCrypt.checkpw(req.password(), user.getPasswordHash()))
                throw AuthException.invalidCredentials();

            String roleName = user.getRole().getName();
            String access = JwtUtil.generateAccessToken(user.getId(), roleName);
            String refresh = JwtUtil.generateRefreshToken(user.getId());

            return new LoginResponse(
                new AuthUserResponse(user.getId(), user.getEmail(), user.getFullName(), roleName),
                access,
                refresh
            );
        } finally {
            em.close();
        }
    }


    public LoginResponse refresh(RefreshRequest req) {
        if (req.refreshToken() == null)
            throw ApiException.badRequest("Thiếu refresh token");
        try {
            Claims claims = JwtUtil.parseRefreshToken(req.refreshToken());
            long userId = Long.parseLong(claims.getSubject());

            EntityManager em = JPAUtil.getEntityManager();
            try {
                User user = em.find(User.class, userId);
                if (user == null || "DISABLED".equals(user.getStatus())) {
                    throw AuthException.unauthorized();
                }
                String roleName = user.getRole().getName();
                String newAccess = JwtUtil.generateAccessToken(user.getId(), roleName);
                String newRefresh = JwtUtil.generateRefreshToken(user.getId());

                return new LoginResponse(
                    new AuthUserResponse(user.getId(), user.getEmail(), user.getFullName(), roleName),
                    newAccess,
                    newRefresh
                );
            } finally {
                em.close();
            }
        } catch (Exception e) {
            throw AuthException.invalidToken();
        }
    }
}
