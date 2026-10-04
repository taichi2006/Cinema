package com.cinema.wallet;

import com.cinema.common.util.JPAUtil;
import com.cinema.user.User;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Data Access Object cho thực thể Wallet.
 */
public class WalletDAO {

    public Optional<Wallet> findByUserId(long userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT w FROM Wallet w WHERE w.user.id = :userId", Wallet.class)
                    .setParameter("userId", userId)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    public Optional<Wallet> findById(long walletId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Wallet.class, walletId));
        } finally {
            em.close();
        }
    }

    public Wallet save(Wallet wallet) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (wallet.getId() == null) {
                em.persist(wallet);
            } else {
                wallet = em.merge(wallet);
            }
            em.getTransaction().commit();
            return wallet;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Khởi tạo ví mặc định cho người dùng trong transaction sẵn có (dùng khi đăng ký tài khoản).
     */
    public Wallet createDefaultWallet(User user, EntityManager em) {
        Wallet wallet = new Wallet(user);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        em.persist(wallet);
        return wallet;
    }
}
