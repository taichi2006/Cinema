package com.cinema.wallet.dao;

import com.cinema.common.util.JPAUtil;
import com.cinema.user.entity.User;
import com.cinema.wallet.entity.Wallet;
import com.cinema.wallet.enums.WalletStatus;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class WalletDAO {

    public Optional<Wallet> findByUserId(long userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT w FROM Wallet w JOIN FETCH w.user WHERE w.user.id = :userId", Wallet.class)
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
            List<Wallet> list = em.createQuery("SELECT w FROM Wallet w JOIN FETCH w.user WHERE w.id = :walletId", Wallet.class)
                    .setParameter("walletId", walletId)
                    .getResultList();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
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
     * Khởi tạo ví rỗng cho người dùng mới đăng kí tài khoản
     */
    public Wallet createDefaultWallet(User user, EntityManager em) {
        Wallet wallet = new Wallet(user);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setStatus(WalletStatus.ACTIVE);
        em.persist(wallet);
        return wallet;
    }
}
