package com.cinema.auth;

import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.user.Role;
import com.cinema.user.User;

import jakarta.persistence.EntityManager;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    public record RegisterRequest(String email, String password, String fullName) {}
    public record LoginRequest(String email, String password) {}
    public record AuthUserResponse(long userId, String email, String fullName, String role) {}
    public record LoginData(AuthUserResponse user, String accessToken) {}

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

    public LoginData login(LoginRequest req) {
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

            return new LoginData(
                new AuthUserResponse(user.getId(), user.getEmail(), user.getFullName(), roleName),
                access
            );
        } finally {
            em.close();
        }
    }
}
