package com.cinema.user.dao;

import com.cinema.common.util.JPAUtil;
import com.cinema.user.criteria.UserVoucherCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserVoucherDAO {

    private static final String BASE_CTE = """
            WITH voucher_usage AS (
                SELECT bv.voucher_id, count(*) AS used_count
                FROM cinema.booking_vouchers bv
                WHERE bv.user_id = :userId AND bv.status = 'USED'
                GROUP BY bv.voucher_id
            ), voucher_data AS (
                SELECT v.voucher_id,
                       v.code,
                       v.description,
                       v.discount_type,
                       v.discount_value,
                       v.max_discount_amount,
                       v.min_order_amount,
                       v.applies_to,
                       v.starts_at,
                       v.expires_at,
                       greatest(v.per_user_limit - coalesce(vu.used_count, 0), 0) AS remaining_uses,
                       CASE
                           WHEN v.status = 'INACTIVE' OR v.expires_at <= CURRENT_TIMESTAMP
                               THEN 'EXPIRED'
                           WHEN greatest(v.per_user_limit - coalesce(vu.used_count, 0), 0) = 0
                               THEN 'USED'
                           ELSE 'AVAILABLE'
                       END AS derived_status
                FROM cinema.user_vouchers uv
                JOIN cinema.vouchers v ON v.voucher_id = uv.voucher_id
                LEFT JOIN voucher_usage vu ON vu.voucher_id = v.voucher_id
                WHERE uv.user_id = :userId
            )
            """;

    public long count(long userId, UserVoucherCriteria query) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            String sql = BASE_CTE + "SELECT count(*) FROM voucher_data"
                    + (query.status() == null ? "" : " WHERE derived_status = :derivedStatus");
            Query nativeQuery = entityManager.createNativeQuery(sql)
                    .setParameter("userId", userId);
            if (query.status() != null) {
                nativeQuery.setParameter("derivedStatus", query.status());
            }
            return ((Number) nativeQuery.getSingleResult()).longValue();
        } finally {
            entityManager.close();
        }
    }

    public List<VoucherRow> findPage(long userId, UserVoucherCriteria query) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            String sql = BASE_CTE + """
                    SELECT voucher_id, code, description, derived_status, discount_type,
                           discount_value, max_discount_amount, min_order_amount, applies_to,
                           remaining_uses, starts_at, expires_at
                    FROM voucher_data
                    """
                    + (query.status() == null ? "" : " WHERE derived_status = :derivedStatus")
                    + " ORDER BY expires_at ASC, voucher_id ASC LIMIT :limit OFFSET :offset";

            Query nativeQuery = entityManager.createNativeQuery(sql)
                    .setParameter("userId", userId)
                    .setParameter("limit", query.size())
                    .setParameter("offset", query.offset());
            if (query.status() != null) {
                nativeQuery.setParameter("derivedStatus", query.status());
            }

            @SuppressWarnings("unchecked")
            List<Object[]> rows = nativeQuery.getResultList();
            return rows.stream().map(this::mapRow).toList();
        } finally {
            entityManager.close();
        }
    }

    public Map<Long, List<String>> findEligibleCinemaIds(List<Long> voucherIds) {
        return findEligibleIds(voucherIds, "cinema.voucher_cinemas", "cinema_id");
    }

    public Map<Long, List<String>> findEligibleMovieIds(List<Long> voucherIds) {
        return findEligibleIds(voucherIds, "cinema.voucher_movies", "movie_id");
    }

    private Map<Long, List<String>> findEligibleIds(
            List<Long> voucherIds,
            String table,
            String valueColumn
    ) {
        Map<Long, List<String>> result = new LinkedHashMap<>();
        voucherIds.forEach(id -> result.put(id, new ArrayList<>()));
        if (voucherIds.isEmpty()) return result;

        StringBuilder placeholders = new StringBuilder();
        for (int index = 0; index < voucherIds.size(); index++) {
            if (index > 0) placeholders.append(", ");
            placeholders.append(":id").append(index);
        }

        String sql = "SELECT voucher_id, " + valueColumn
                + " FROM " + table
                + " WHERE voucher_id IN (" + placeholders + ")"
                + " ORDER BY voucher_id, " + valueColumn;

        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            Query nativeQuery = entityManager.createNativeQuery(sql);
            for (int index = 0; index < voucherIds.size(); index++) {
                nativeQuery.setParameter("id" + index, voucherIds.get(index));
            }

            @SuppressWarnings("unchecked")
            List<Object[]> rows = nativeQuery.getResultList();
            for (Object[] row : rows) {
                long voucherId = ((Number) row[0]).longValue();
                result.computeIfAbsent(voucherId, ignored -> new ArrayList<>())
                        .add(String.valueOf(row[1]));
            }
            return result;
        } finally {
            entityManager.close();
        }
    }

    private VoucherRow mapRow(Object[] row) {
        return new VoucherRow(
                ((Number) row[0]).longValue(),
                String.valueOf(row[1]),
                row[2] == null ? null : String.valueOf(row[2]),
                String.valueOf(row[3]),
                String.valueOf(row[4]),
                ((Number) row[5]).longValue(),
                row[6] == null ? null : ((Number) row[6]).longValue(),
                ((Number) row[7]).longValue(),
                String.valueOf(row[8]),
                ((Number) row[9]).intValue(),
                UserBookingDAO.toInstant(row[10]),
                UserBookingDAO.toInstant(row[11])
        );
    }

    public record VoucherRow(
            long id,
            String code,
            String description,
            String status,
            String discountType,
            long discountValue,
            Long maxDiscountAmount,
            long minOrderAmount,
            String appliesTo,
            int remainingUses,
            Instant startsAt,
            Instant expiresAt
    ) {
    }
}
