package com.cinema.auth;

import com.cinema.common.util.JPAUtil;
import com.cinema.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class AuthDAO {

    public boolean existsByEmail(String email) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            Long count = entityManager.createQuery(
                            "SELECT count(user) FROM User user WHERE user.email = :email",
                            Long.class
                    )
                    .setParameter("email", email)
                    .getSingleResult();
            return count > 0;
        } finally {
            entityManager.close();
        }
    }

    public User createUser(String email, String passwordHash, String fullName) {
        return createUser(email, passwordHash, fullName, null, null);
    }

    public User createUser(
            String email,
            String passwordHash,
            String fullName,
            String phone,
            LocalDate dob
    ) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();

            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordHash);
            user.setFullName(fullName);
            user.setPhone(phone);
            user.setDob(dob);
            user.setStatus("ACTIVE");
            entityManager.persist(user);

            // Khởi tạo ví rỗng cho user trong bảng cinema.wallets
            entityManager.createNativeQuery(
                            """
                            INSERT INTO cinema.wallets (user_id, balance, status)
                            VALUES (:userId, 0.00, 'ACTIVE')
                            """
                    )
                    .setParameter("userId", user.getId())
                    .executeUpdate();

            transaction.commit();
            return user;
        } catch (RuntimeException exception) {
            rollback(transaction);
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    public Optional<User> findUserByEmail(String email) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            return entityManager.createQuery(
                            """
                            SELECT user
                            FROM User user
                            WHERE user.email = :email
                            """,
                            User.class
                    )
                    .setParameter("email", email)
                    .getResultStream()
                    .findFirst();
        } finally {
            entityManager.close();
        }
    }

    public Optional<User> findUserById(long userId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            return entityManager.createQuery(
                            """
                            SELECT user
                            FROM User user
                            WHERE user.id = :userId
                            """,
                            User.class
                    )
                    .setParameter("userId", userId)
                    .getResultStream()
                    .findFirst();
        } finally {
            entityManager.close();
        }
    }

    public void saveRefreshToken(
            long userId,
            String tokenHash,
            Instant expiresAt,
            String userAgent,
            String ipAddress
    ) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.createNativeQuery(
                            """
                            INSERT INTO cinema.refresh_tokens (user_id, token_hash, expires_at, user_agent, ip_address, revoked)
                            VALUES (:userId, :tokenHash, :expiresAt, :userAgent, :ipAddress, false)
                            """
                    )
                    .setParameter("userId", userId)
                    .setParameter("tokenHash", tokenHash)
                    .setParameter("expiresAt", Timestamp.from(expiresAt))
                    .setParameter("userAgent", userAgent)
                    .setParameter("ipAddress", ipAddress)
                    .executeUpdate();
            transaction.commit();
        } catch (RuntimeException exception) {
            rollback(transaction);
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    public boolean isRefreshTokenValid(String tokenHash, long userId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            Number count = (Number) entityManager.createNativeQuery(
                            """
                            SELECT count(*)
                            FROM cinema.refresh_tokens
                            WHERE token_hash = :tokenHash
                              AND user_id = :userId
                              AND revoked = false
                              AND expires_at > CURRENT_TIMESTAMP
                            """
                    )
                    .setParameter("tokenHash", tokenHash)
                    .setParameter("userId", userId)
                    .getSingleResult();
            return count != null && count.longValue() > 0;
        } finally {
            entityManager.close();
        }
    }

    public boolean revokeRefreshToken(String tokenHash) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            int updated = entityManager.createNativeQuery(
                            """
                            UPDATE cinema.refresh_tokens
                            SET revoked = true, revoked_at = CURRENT_TIMESTAMP
                            WHERE token_hash = :tokenHash AND revoked = false
                            """
                    )
                    .setParameter("tokenHash", tokenHash)
                    .executeUpdate();
            transaction.commit();
            return updated > 0;
        } catch (RuntimeException exception) {
            rollback(transaction);
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    public void revokeAllUserTokens(long userId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            entityManager.createNativeQuery(
                            """
                            UPDATE cinema.refresh_tokens
                            SET revoked = true, revoked_at = CURRENT_TIMESTAMP
                            WHERE user_id = :userId AND revoked = false
                            """
                    )
                    .setParameter("userId", userId)
                    .executeUpdate();
            transaction.commit();
        } catch (RuntimeException exception) {
            rollback(transaction);
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    public boolean updatePasswordAndRevokeTokens(
            long userId,
            String expectedPasswordHash,
            String newPasswordHash
    ) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
            if (user == null || !Objects.equals(user.getPasswordHash(), expectedPasswordHash)) {
                transaction.rollback();
                return false;
            }

            user.setPasswordHash(newPasswordHash);

            entityManager.createNativeQuery(
                            """
                            UPDATE cinema.refresh_tokens
                            SET revoked = true, revoked_at = CURRENT_TIMESTAMP
                            WHERE user_id = :userId AND revoked = false
                            """
                    )
                    .setParameter("userId", userId)
                    .executeUpdate();

            transaction.commit();
            return true;
        } catch (RuntimeException exception) {
            rollback(transaction);
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    private void rollback(EntityTransaction transaction) {
        if (transaction.isActive()) {
            transaction.rollback();
        }
    }
}
