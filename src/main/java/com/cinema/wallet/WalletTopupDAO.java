package com.cinema.wallet;

import com.cinema.common.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object cho thực thể WalletTopup.
 */
public class WalletTopupDAO {

    public WalletTopup save(WalletTopup topup) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            if (topup.getId() == null) {
                em.persist(topup);
            } else {
                topup = em.merge(topup);
            }
            tx.commit();
            return topup;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    public Optional<WalletTopup> findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<WalletTopup> list = em.createQuery(
                    "SELECT t FROM WalletTopup t JOIN FETCH t.wallet w JOIN FETCH w.user WHERE t.id = :id",
                    WalletTopup.class
            )
            .setParameter("id", id)
            .getResultList();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } finally {
            em.close();
        }
    }

    public List<WalletTopup> findPendingTopUps(int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT t FROM WalletTopup t JOIN FETCH t.wallet w JOIN FETCH w.user WHERE t.status = 'PENDING' ORDER BY t.createdAt DESC",
                    WalletTopup.class
            )
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();
        } finally {
            em.close();
        }
    }

    public long countPendingTopUps() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                    "SELECT COUNT(t) FROM WalletTopup t WHERE t.status = 'PENDING'",
                    Long.class
            ).getSingleResult();
        } finally {
            em.close();
        }
    }
}
