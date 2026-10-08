package com.cinema.user.dao;

import com.cinema.common.util.JPAUtil;
import com.cinema.user.criteria.UserBookingCriteria;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.ArrayList;
import java.util.List;

public class UserBookingDAO {

    public long count(long userId, UserBookingCriteria query) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder("""
                    SELECT count(*)
                    FROM cinema.bookings b
                    WHERE b.user_id = :userId
                    """);
            appendFilters(sql, query);

            Query nativeQuery = entityManager.createNativeQuery(sql.toString());
            bindFilters(nativeQuery, userId, query);
            return ((Number) nativeQuery.getSingleResult()).longValue();
        } finally {
            entityManager.close();
        }
    }

    public List<BookingRow> findPage(long userId, UserBookingCriteria query) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder("""
                    SELECT b.booking_id,
                           b.status,
                           b.total_amount
                    FROM cinema.bookings b
                    WHERE b.user_id = :userId
                    """);
            appendFilters(sql, query);

            sql.append(" ORDER BY b.created_at ");
            sql.append(query.sortDirection() == UserBookingCriteria.SortDirection.ASC ? "ASC" : "DESC");
            sql.append(" LIMIT :limit OFFSET :offset ");

            Query nativeQuery = entityManager.createNativeQuery(sql.toString());
            bindFilters(nativeQuery, userId, query);
            nativeQuery.setParameter("limit", query.size());
            nativeQuery.setParameter("offset", query.offset());

            @SuppressWarnings("unchecked")
            List<Object[]> results = nativeQuery.getResultList();
            List<BookingRow> rows = new ArrayList<>(results.size());
            for (Object[] row : results) {
                long bookingId = ((Number) row[0]).longValue();
                String status = (String) row[1];
                long totalAmount = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                rows.add(new BookingRow(bookingId, status, totalAmount));
            }
            return rows;
        } finally {
            entityManager.close();
        }
    }

    private void appendFilters(StringBuilder sql, UserBookingCriteria query) {
        if (query.status() != null) {
            sql.append(" AND b.status = :status ");
        }
        if (query.fromInclusive() != null) {
            sql.append(" AND b.created_at >= :fromInclusive ");
        }
        if (query.toExclusive() != null) {
            sql.append(" AND b.created_at < :toExclusive ");
        }
    }

    private void bindFilters(Query nativeQuery, long userId, UserBookingCriteria query) {
        nativeQuery.setParameter("userId", userId);
        if (query.status() != null) {
            nativeQuery.setParameter("status", query.status());
        }
        if (query.fromInclusive() != null) {
            nativeQuery.setParameter("fromInclusive", java.sql.Timestamp.from(query.fromInclusive()));
        }
        if (query.toExclusive() != null) {
            nativeQuery.setParameter("toExclusive", java.sql.Timestamp.from(query.toExclusive()));
        }
    }

    public record BookingRow(
            long bookingId,
            String status,
            long totalAmount
    ) {}
}
