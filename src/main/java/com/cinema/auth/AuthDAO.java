package com.cinema.auth;

import com.cinema.common.util.JPAUtil;
import com.cinema.user.entity.User;
import com.cinema.wallet.entity.Wallet;
import com.cinema.wallet.enums.WalletStatus;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class AuthDAO {

    public boolean existsByEmail(String email) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery("SELECT count(u) FROM User u WHERE u.email = :email", Long.class)
                    .setParameter("email", email)
                    .getSingleResult();
            return count != null && count > 0;
        } finally {
            em.close();
        }
    }

    public User createUser(String email, String passwordHash, String fullName) {
        return createUser(email, passwordHash, fullName, null, null);
    }

    public User createUser(String email, String passwordHash, String fullName, String phone, LocalDate dob) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            User user = new User();
            user.setEmail(email);
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
            throw e;
        } finally {
            em.close();
        }
    }

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

    public void saveRefreshToken(long userId, String token, Instant expiresAt) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            em.createNativeQuery(
                    "INSERT INTO cinema.refresh_tokens (user_id, token_hash, expires_at, revoked) " +
                    "VALUES (:userId, :token, :expiresAt, false)"
            )
            .setParameter("userId", userId)
            .setParameter("token", token)
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
            Number count = (Number) em.createNativeQuery(
                    "SELECT count(*) FROM cinema.refresh_tokens " +
                    "WHERE token_hash = :token AND user_id = :userId AND revoked = false AND expires_at > CURRENT_TIMESTAMP"
            )
            .setParameter("token", token)
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
            int updated = em.createNativeQuery(
                    "UPDATE cinema.refresh_tokens SET revoked = true, revoked_at = CURRENT_TIMESTAMP " +
                    "WHERE token_hash = :token AND revoked = false"
            )
            .setParameter("token", token)
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
            return true;
        } catch (RuntimeException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
