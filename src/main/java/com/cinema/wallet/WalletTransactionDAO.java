package com.cinema.wallet;

import com.cinema.common.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object cho thực thể WalletTransaction.
 */
public class WalletTransactionDAO {

    public Optional<WalletTransaction> findById(long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(WalletTransaction.class, id));
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
     * Lấy lịch sử giao dịch đã thành công (SUCCEEDED hoặc SUCCESSFUL) của một ví, có hỗ trợ lọc và phân trang.
     */
    public List<WalletTransaction> findHistory(long walletId, TransactionType type, Instant from, Instant to, int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT tx FROM WalletTransaction tx WHERE tx.wallet.id = :walletId " +
                    "AND tx.status IN (com.cinema.wallet.TransactionStatus.SUCCEEDED, com.cinema.wallet.TransactionStatus.SUCCESSFUL) "
            );

            if (type != null) {
                jpql.append("AND tx.transactionType = :type ");
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

            if (type != null) {
                query.setParameter("type", type);
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
     * Đếm tổng số giao dịch thành công theo điều kiện lọc để tính PageMeta.
     */
    public long countHistory(long walletId, TransactionType type, Instant from, Instant to) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT count(tx) FROM WalletTransaction tx WHERE tx.wallet.id = :walletId " +
                    "AND tx.status IN (com.cinema.wallet.TransactionStatus.SUCCEEDED, com.cinema.wallet.TransactionStatus.SUCCESSFUL) "
            );

            if (type != null) {
                jpql.append("AND tx.transactionType = :type ");
            }
            if (from != null) {
                jpql.append("AND tx.createdAt >= :from ");
            }
            if (to != null) {
                jpql.append("AND tx.createdAt <= :to ");
            }

            TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class)
                    .setParameter("walletId", walletId);

            if (type != null) {
                query.setParameter("type", type);
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

    /**
     * Lấy danh sách các yêu cầu nạp tiền đang ở trạng thái PENDING cho Admin duyệt.
     */
    public List<WalletTransaction> findPendingTopUps(int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql = "SELECT tx FROM WalletTransaction tx " +
                    "JOIN FETCH tx.wallet w " +
                    "JOIN FETCH w.user u " +
                    "WHERE tx.transactionType = :type AND tx.status = :status " +
                    "ORDER BY tx.createdAt ASC, tx.id ASC";

            return em.createQuery(jpql, WalletTransaction.class)
                    .setParameter("type", TransactionType.TOP_UP)
                    .setParameter("status", TransactionStatus.PENDING)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    /**
     * Đếm tổng số yêu cầu nạp tiền PENDING.
     */
    public long countPendingTopUps() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT count(tx) FROM WalletTransaction tx " +
                    "WHERE tx.transactionType = :type AND tx.status = :status", Long.class)
                    .setParameter("type", TransactionType.TOP_UP)
                    .setParameter("status", TransactionStatus.PENDING)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }
}
