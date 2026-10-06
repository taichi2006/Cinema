package com.cinema.auth;

import com.cinema.common.util.JPAUtil;
import com.cinema.user.entity.Role;
import com.cinema.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;

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

    public Optional<Long> findRoleIdByName(String roleName) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            return entityManager.createQuery(
                            "SELECT role.id FROM Role role WHERE role.name = :roleName",
                            Long.class
                    )
                    .setParameter("roleName", roleName)
                    .getResultStream()
                    .findFirst();
        } finally {
            entityManager.close();
        }
    }

    public User createUser(
            String email,
            String passwordHash,
            String fullName,
            long roleId
    ) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();

            User user = new User();
            user.setEmail(email);
            user.setPasswordHash(passwordHash);
            user.setFullName(fullName);
            user.setRole(entityManager.getReference(Role.class, roleId));
            entityManager.persist(user);

            transaction.commit();
            return user;
        } catch (RuntimeException exception) {
            rollback(transaction);
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    public Optional<User> findUserWithRoleByEmail(String email) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            return entityManager.createQuery(
                            """
                            SELECT user
                            FROM User user
                            JOIN FETCH user.role
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

    public Optional<User> findUserWithRoleById(long userId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            return entityManager.createQuery(
                            """
                            SELECT user
                            FROM User user
                            JOIN FETCH user.role
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
            int currentAuthVersion = user.getAuthVersion() == null ? 0 : user.getAuthVersion();
            user.setAuthVersion(currentAuthVersion + 1);

            entityManager.createNativeQuery(
                            """
                            UPDATE cinema.refresh_tokens
                            SET revoked = true
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
