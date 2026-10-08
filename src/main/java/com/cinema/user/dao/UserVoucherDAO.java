package com.cinema.user.dao;

import com.cinema.common.util.JPAUtil;
import com.cinema.user.criteria.UserVoucherCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.List;

public class UserVoucherDAO {

    public long count(long userId, UserVoucherCriteria query) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder("""
                    SELECT count(*)
                    FROM cinema.user_vouchers uv
                    JOIN cinema.vouchers v ON v.voucher_id = uv.voucher_id
                    WHERE uv.user_id = :userId
                    """);
            if (query.status() != null) {
                sql.append(" AND uv.status = :status ");
            }
            Query nativeQuery = entityManager.createNativeQuery(sql.toString())
                    .setParameter("userId", userId);
            if (query.status() != null) {
                nativeQuery.setParameter("status", query.status());
            }
            return ((Number) nativeQuery.getSingleResult()).longValue();
        } finally {
            entityManager.close();
        }
    }

    public List<VoucherRow> findPage(long userId, UserVoucherCriteria query) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder("""
                    SELECT uv.voucher_id,
                           v.code,
                           uv.status
                    FROM cinema.user_vouchers uv
                    JOIN cinema.vouchers v ON v.voucher_id = uv.voucher_id
                    WHERE uv.user_id = :userId
                    """);
            if (query.status() != null) {
                sql.append(" AND uv.status = :status ");
            }
            sql.append(" ORDER BY uv.assigned_at DESC LIMIT :limit OFFSET :offset ");

            Query nativeQuery = entityManager.createNativeQuery(sql.toString())
                    .setParameter("userId", userId)
                    .setParameter("limit", query.size())
                    .setParameter("offset", query.offset());
            if (query.status() != null) {
                nativeQuery.setParameter("status", query.status());
            }

            @SuppressWarnings("unchecked")
            List<Object[]> results = nativeQuery.getResultList();
            List<VoucherRow> rows = new ArrayList<>(results.size());
            for (Object[] row : results) {
                long voucherId = ((Number) row[0]).longValue();
                String code = (String) row[1];
                String status = (String) row[2];
                rows.add(new VoucherRow(voucherId, code, status));
            }
            return rows;
        } finally {
            entityManager.close();
        }
    }

    public record VoucherRow(
            long voucherId,
            String code,
            String status
    ) {}
}
