package com.cinema.user.dao;

import com.cinema.common.util.JPAUtil;
import com.cinema.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public class UserDAO {

    public Optional<User> findProfileById(long userId) {
        EntityManager entityManager = JPAUtil.getEntityManager();

        try {
            return findProfileById(entityManager, userId);
        } finally {
            entityManager.close();
        }
    }

    public Optional<User> updateProfile(long userId, UpdateProfileCommand command) {
        Objects.requireNonNull(command, "Update profile command must not be null");

        EntityManager entityManager = JPAUtil.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();

        try {
            transaction.begin();

            Optional<User> foundUser = findProfileById(entityManager, userId);
            if (foundUser.isEmpty()) {
                transaction.rollback();
                return Optional.empty();
            }

            User user = foundUser.get();

            if (command.fullNameProvided()) {
                user.setFullName(command.fullName());
            }
            if (command.phoneProvided()) {
                user.setPhone(command.phone());
            }
            if (command.dobProvided()) {
                user.setDob(command.dob());
            }

            transaction.commit();
            return Optional.of(user);
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    private Optional<User> findProfileById(EntityManager entityManager, long userId) {
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
    }

    public record UpdateProfileCommand(
            boolean fullNameProvided,
            String fullName,
            boolean phoneProvided,
            String phone,
            boolean dobProvided,
            LocalDate dob
    ) {
    }
}
