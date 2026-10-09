package com.cinema.wallet.dao;

import com.cinema.common.util.JPAUtil;
import com.cinema.wallet.entity.WalletTransaction;
import com.cinema.wallet.enums.TransactionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class WalletTransactionDAO {

    public Optional<WalletTransaction> findById(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<WalletTransaction> list = em.createQuery(
                    "SELECT tx FROM WalletTransaction tx JOIN FETCH tx.wallet w JOIN FETCH w.user WHERE tx.id = :id",
                    WalletTransaction.class
            )
            .setParameter("id", id)
            .getResultList();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } finally {
            em.close();
        }
    }

    public WalletTransaction save(WalletTransaction tx) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            if (tx.getId() == null) {
                em.persist(tx);
            } else {
                tx = em.merge(tx);
            }
            em.getTransaction().commit();
            return tx;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public WalletTransaction save(WalletTransaction tx, EntityManager em) {
        if (tx.getId() == null) {
            em.persist(tx);
            return tx;
        } else {
            return em.merge(tx);
        }
    }

    /**
     * Lấy lịch sử bút toán của một ví, có hỗ trợ lọc theo loại và khoảng thời gian, phân trang mới nhất trước.
     */
    public List<WalletTransaction> findHistory(long walletId, String type, Instant from, Instant to, int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT tx FROM WalletTransaction tx WHERE tx.wallet.id = :walletId "
            );

            TransactionType txType = null;
            if (type != null && !type.isBlank()) {
                try {
                    txType = TransactionType.valueOf(type.trim().toUpperCase());
                    jpql.append("AND tx.type = :type ");
                } catch (IllegalArgumentException ignored) {
                    // Nếu type không đúng enum, để query rỗng hoặc handle
                    jpql.append("AND 1 = 0 ");
                }
            }
            if (from != null) {
                jpql.append("AND tx.createdAt >= :from ");
            }
            if (to != null) {
                jpql.append("AND tx.createdAt <= :to ");
            }

            jpql.append("ORDER BY tx.createdAt DESC, tx.id DESC");

            TypedQuery<WalletTransaction> query = em.createQuery(jpql.toString(), WalletTransaction.class)
                    .setParameter("walletId", walletId);

            if (txType != null) {
                query.setParameter("type", txType);
            }
            if (from != null) {
                query.setParameter("from", from);
            }
            if (to != null) {
                query.setParameter("to", to);
            }

            query.setFirstResult(page * size);
            query.setMaxResults(size);

            return query.getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Đếm tổng số bút toán theo điều kiện lọc để tính PageMeta.
     */
    public long countHistory(long walletId, String type, Instant from, Instant to) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT count(tx) FROM WalletTransaction tx WHERE tx.wallet.id = :walletId "
            );

            TransactionType txType = null;
            if (type != null && !type.isBlank()) {
                try {
                    txType = TransactionType.valueOf(type.trim().toUpperCase());
                    jpql.append("AND tx.type = :type ");
                } catch (IllegalArgumentException ignored) {
                    return 0L;
                }
            }
            if (from != null) {
                jpql.append("AND tx.createdAt >= :from ");
            }
            if (to != null) {
                jpql.append("AND tx.createdAt <= :to ");
            }

            TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class)
                    .setParameter("walletId", walletId);

            if (txType != null) {
                query.setParameter("type", txType);
            }
            if (from != null) {
                query.setParameter("from", from);
            }
            if (to != null) {
                query.setParameter("to", to);
            }

            return query.getSingleResult();
        } finally {
            em.close();
        }
    }
}
