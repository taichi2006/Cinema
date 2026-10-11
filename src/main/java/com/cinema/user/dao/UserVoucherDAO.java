package com.cinema.user.dao;

import com.cinema.common.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.List;

public class UserVoucherDAO {

    public record VoucherRow(long voucherId, String code, String status) {}

    public long count(long userId, String status) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder("SELECT count(*) FROM cinema.user_vouchers uv WHERE uv.user_id = :userId");
            if (status != null && !status.isBlank()) sql.append(" AND uv.status = :status");

            Query q = em.createNativeQuery(sql.toString()).setParameter("userId", userId);
            if (status != null && !status.isBlank()) q.setParameter("status", status.trim());
            return ((Number) q.getSingleResult()).longValue();
        } finally {
            em.close();
        }
    }

    @SuppressWarnings("unchecked")
    public List<VoucherRow> findPage(long userId, String status, int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT uv.voucher_id, v.code, uv.status FROM cinema.user_vouchers uv " +
                    "JOIN cinema.vouchers v ON v.voucher_id = uv.voucher_id WHERE uv.user_id = :userId"
            );
            if (status != null && !status.isBlank()) sql.append(" AND uv.status = :status");
            sql.append(" ORDER BY uv.assigned_at DESC LIMIT :limit OFFSET :offset");

            Query q = em.createNativeQuery(sql.toString())
                    .setParameter("userId", userId)
                    .setParameter("limit", size)
                    .setParameter("offset", (long) page * size);
            if (status != null && !status.isBlank()) q.setParameter("status", status.trim());

            List<Object[]> results = q.getResultList();
            return results.stream().map(row -> new VoucherRow(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2]
            )).toList();
        } finally {
            em.close();
        }
    }
}
