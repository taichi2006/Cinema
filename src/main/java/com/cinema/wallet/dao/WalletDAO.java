package com.cinema.wallet.dao;

import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.user.entity.User;
import com.cinema.wallet.entity.Wallet;
import com.cinema.wallet.entity.WalletTransaction;
import com.cinema.wallet.enums.TransactionStatus;
import com.cinema.wallet.enums.TransactionType;
import com.cinema.wallet.enums.WalletStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class WalletDAO {

    public Optional<Wallet> findByUserIdForUpdate(long userId, EntityManager em) {
        return em.createQuery(
                        "SELECT w FROM Wallet w WHERE w.user.id = :userId",
                        Wallet.class)
                .setParameter("userId", userId)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }

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

    public Wallet getOrCreate(long userId) {
        Optional<Wallet> existing = findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            User user = em.find(User.class, userId);
            if (user == null) {
                throw ApiException.notFound("Không tìm thấy người dùng #" + userId);
            }
            Wallet wallet = new Wallet(user);
            wallet.setBalance(BigDecimal.ZERO);
            wallet.setStatus(WalletStatus.ACTIVE);
            em.persist(wallet);
            em.getTransaction().commit();
            return wallet;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public WalletTransaction topUp(long userId, long amount) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Wallet wallet = em.createQuery("SELECT w FROM Wallet w JOIN FETCH w.user WHERE w.user.id = :userId", Wallet.class)
                    .setParameter("userId", userId)
                    .getResultStream().findFirst().orElse(null);

            if (wallet == null) {
                User user = em.find(User.class, userId);
                if (user == null) {
                    throw ApiException.notFound("Không tìm thấy người dùng #" + userId);
                }
                wallet = new Wallet(user);
                wallet.setBalance(BigDecimal.ZERO);
                wallet.setStatus(WalletStatus.ACTIVE);
                em.persist(wallet);
            }

            if (wallet.getStatus() == WalletStatus.SUSPENDED) {
                throw ApiException.forbidden("Ví của bạn đang bị tạm khóa (SUSPENDED)");
            }

            BigDecimal amountBd = BigDecimal.valueOf(amount);
            wallet.setBalance(wallet.getBalance().add(amountBd));
            em.merge(wallet);

            WalletTransaction tx = new WalletTransaction();
            tx.setWallet(wallet);
            tx.setType(TransactionType.ADD_MONEY);
            tx.setStatus(TransactionStatus.SUCCESSFUL);
            tx.setAmount(amountBd);
            tx.setDescription("Nạp tiền vào ví thành công");
            em.persist(tx);

            em.getTransaction().commit();
            return tx;
        } catch (ApiException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ApiException.internal("Lỗi nạp tiền vào ví: " + e.getMessage());
        } finally {
            em.close();
        }
    }
}
