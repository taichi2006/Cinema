package com.cinema.user.dao;

import com.cinema.common.util.JPAUtil;
import com.cinema.user.entity.User;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.Optional;

public class UserDAO {

    public record UpdateProfileCommand(
            boolean fullNameProvided, String fullName,
            boolean phoneProvided, String phone,
            boolean dobProvided, LocalDate dob
    ) {}

    // 1. Tìm thông tin người dùng theo ID
    public Optional<User> findProfileById(long userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(User.class, userId));
        } finally {
            em.close();
        }
    }

    // 2. Cập nhật hồ sơ người dùng
    public Optional<User> updateProfile(long userId, UpdateProfileCommand command) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, userId);
            if (user == null) {
                em.getTransaction().rollback();
                return Optional.empty();
            }

            if (command.fullNameProvided()) user.setFullName(command.fullName());
            if (command.phoneProvided()) user.setPhone(command.phone());
            if (command.dobProvided()) user.setDob(command.dob());

            em.merge(user);
            em.getTransaction().commit();
            return Optional.of(user);
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
