package com.cinema.auth.dao;

import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.user.entity.User;
import com.cinema.wallet.entity.Wallet;
import com.cinema.wallet.enums.WalletStatus;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class AuthDAO {

    // 1. Kiểm tra sự tồn tại của Email
    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery("SELECT count(u) FROM User u WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email))", Long.class)
                    .setParameter("email", email.trim())
                    .getSingleResult();
            return count != null && count > 0;
        } finally {
            em.close();
        }
    }

    // 2. Tạo người dùng mới và tự động mở ví
    public User createUser(String email, String passwordHash, String fullName) {
        return createUser(email, passwordHash, fullName, null, null);
    }

    public User createUser(String email, String passwordHash, String fullName, String phone, LocalDate dob) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            User user = new User();
            user.setEmail(email.trim().toLowerCase());
            user.setPasswordHash(passwordHash);
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setDob(dob);
            user.setStatus("ACTIVE");
            em.persist(user);

            Wallet wallet = new Wallet(user);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setStatus(WalletStatus.ACTIVE);
            em.persist(wallet);

            em.getTransaction().commit();
            return user;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String rootMsg = root.getMessage() != null ? root.getMessage().toLowerCase() : "";
            if (msg.contains("unique") || msg.contains("duplicate") || msg.contains("users_email_key")
                    || rootMsg.contains("unique") || rootMsg.contains("duplicate") || rootMsg.contains("users_email_key")
                    || rootMsg.contains("23505")) {
                throw ApiException.conflict("Email đã được đăng ký");
            }
            throw e;
        } finally {
            em.close();
        }
    }

    // 3. Tìm kiếm người dùng
    public Optional<User> findUserByEmail(String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    public Optional<User> findUserById(long userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(User.class, userId));
        } finally {
            em.close();
        }
    }

    // 4. Quản lý Refresh Token trong CSDL
    public void saveRefreshToken(long userId, String token, Instant expiresAt) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(
                    "INSERT INTO cinema.refresh_tokens (user_id, token_hash, expires_at, revoked) " +
                    "VALUES (:userId, :token, :expiresAt, false)"
            )
            .setParameter("userId", userId)
            .setParameter("token", toTokenHash(token))
            .setParameter("expiresAt", Timestamp.from(expiresAt))
            .executeUpdate();
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean isRefreshTokenValid(String token, long userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String hash = toTokenHash(token);
            Number count = (Number) em.createNativeQuery(
                    "SELECT count(*) FROM cinema.refresh_tokens " +
                    "WHERE (token_hash = :hash OR token_hash = :rawToken) AND user_id = :userId AND revoked = false AND expires_at > CURRENT_TIMESTAMP"
            )
            .setParameter("hash", hash)
            .setParameter("rawToken", token)
            .setParameter("userId", userId)
            .getSingleResult();
            return count != null && count.longValue() > 0;
        } finally {
            em.close();
        }
    }

    public boolean revokeRefreshToken(String token) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            String hash = toTokenHash(token);
            int updated = em.createNativeQuery(
                    "UPDATE cinema.refresh_tokens SET revoked = true, revoked_at = CURRENT_TIMESTAMP " +
                    "WHERE (token_hash = :hash OR token_hash = :rawToken) AND revoked = false"
            )
            .setParameter("hash", hash)
            .setParameter("rawToken", token)
            .executeUpdate();
            em.getTransaction().commit();
            return updated > 0;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public void revokeAllUserTokens(long userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(
                    "UPDATE cinema.refresh_tokens SET revoked = true, revoked_at = CURRENT_TIMESTAMP " +
                    "WHERE user_id = :userId AND revoked = false"
            )
            .setParameter("userId", userId)
            .executeUpdate();
            em.getTransaction().commit();
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean updatePasswordAndRevokeTokens(long userId, String expectedPasswordHash, String newPasswordHash) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, userId);
            if (user == null || !Objects.equals(user.getPasswordHash(), expectedPasswordHash)) {
                em.getTransaction().rollback();
                return false;
            }

            user.setPasswordHash(newPasswordHash);

            em.createNativeQuery(
                    "UPDATE cinema.refresh_tokens SET revoked = true, revoked_at = CURRENT_TIMESTAMP " +
                    "WHERE user_id = :userId AND revoked = false"
            )
            .setParameter("userId", userId)
            .executeUpdate();

            em.getTransaction().commit();
            invalidateTokenVersion(userId);
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    private static final java.util.concurrent.ConcurrentHashMap<Long, Integer> TOKEN_VERSIONS = new java.util.concurrent.ConcurrentHashMap<>();

    // 5. Quản lý Token Version (Đăng nhập 1 thiết bị)
    public int incrementAndGetTokenVersion(long userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Object res = em.createNativeQuery(
                    "UPDATE cinema.users SET token_version = COALESCE(token_version, 1) + 1 " +
                    "WHERE user_id = :userId RETURNING token_version"
            ).setParameter("userId", userId).getSingleResult();
            em.getTransaction().commit();

            int newVer = ((Number) res).intValue();
            TOKEN_VERSIONS.put(userId, newVer);
            return newVer;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public boolean isTokenVersionValid(long userId, Integer tokenVersion) {
        if (tokenVersion == null) {
            return false;
        }
        Integer cached = TOKEN_VERSIONS.get(userId);
        if (cached != null) {
            return cached.equals(tokenVersion);
        }
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Object res = em.createNativeQuery(
                    "SELECT COALESCE(token_version, 1) FROM cinema.users WHERE user_id = :userId"
            ).setParameter("userId", userId).getResultStream().findFirst().orElse(null);
            if (res != null) {
                int dbVer = ((Number) res).intValue();
                TOKEN_VERSIONS.put(userId, dbVer);
                return dbVer == tokenVersion;
            }
            return false;
        } finally {
            em.close();
        }
    }

    public void invalidateTokenVersion(long userId) {
        TOKEN_VERSIONS.remove(userId);
        try {
            incrementAndGetTokenVersion(userId);
        } catch (Exception ignored) {}
    }

    private static String toTokenHash(String token) {
        if (token == null) return null;
        if (token.length() <= 64) {
            return token;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return token.substring(0, Math.min(token.length(), 255));
        }
    }
}
