package com.cinema.user;

import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import jakarta.persistence.EntityManager;
import org.mindrot.jbcrypt.BCrypt;

public class UserService {

    // ── Request / Response ────────────────────────────────────────────────────

    public record UpdateProfileRequest(String fullName) {}
    public record ChangePasswordRequest(String oldPassword, String newPassword) {}

    public record UserResponse(long userId, String email, String fullName, String role, String createdAt) {}

    // ── getMe ─────────────────────────────────────────────────────────────────

    public UserResponse getMe(long userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, userId);
            if (user == null)
                throw ApiException.notFound("Người dùng không tồn tại");
            return mapUser(user);
        } finally {
            em.close();
        }
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    public UserResponse updateProfile(long userId, UpdateProfileRequest req) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, userId);
            if (user == null)
                throw ApiException.notFound("Người dùng không tồn tại");

            user.setFullName(req.fullName() != null ? req.fullName().trim() : null);
            em.getTransaction().commit();

            return mapUser(user);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            if (e instanceof ApiException) throw e;
            throw ApiException.internal("Lỗi server: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    // ── changePassword ────────────────────────────────────────────────────────

    public void changePassword(long userId, ChangePasswordRequest req) {
        if (req.oldPassword() == null || req.newPassword() == null)
            throw ApiException.badRequest("Thiếu oldPassword hoặc newPassword");
        if (req.newPassword().length() < 8)
            throw ApiException.badRequest("Mật khẩu mới phải có ít nhất 8 ký tự");

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, userId);
            if (user == null)
                throw ApiException.notFound("Người dùng không tồn tại");

            if (!BCrypt.checkpw(req.oldPassword(), user.getPasswordHash()))
                throw ApiException.badRequest("Mật khẩu cũ không đúng");

            user.setPasswordHash(BCrypt.hashpw(req.newPassword(), BCrypt.gensalt(12)));
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            if (e instanceof ApiException) throw e;
            throw ApiException.internal("Lỗi server: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private UserResponse mapUser(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getRole().getName(),
            user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }
}
