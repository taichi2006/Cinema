package com.cinema.user.dao;

import com.cinema.common.util.JPAUtil;
import com.cinema.user.criteria.UserBookingCriteria;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

public class UserBookingDAO {

    private static final ObjectMapper JSON = new ObjectMapper();

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
                           m.title,
                           c.cinema_name,
                           r.room_name,
                           s.starts_at,
                           COALESCE(
                               jsonb_agg(bs.seat_label ORDER BY bs.seat_label)
                                   FILTER (WHERE bs.booking_seat_id IS NOT NULL
                                           AND bs.status <> 'RELEASED'),
                               '[]'::jsonb
                           )::text AS seat_labels,
                           COALESCE(b.total_amount, 0),
                           b.currency,
                           b.created_at
                    FROM cinema.bookings b
                    JOIN cinema.showtimes s ON s.showtime_id = b.showtime_id
                    JOIN cinema.movies m ON m.movie_id = s.movie_id
                    JOIN cinema.rooms r ON r.room_id = s.room_id
                    JOIN cinema.cinemas c ON c.cinema_id = r.cinema_id
                    LEFT JOIN cinema.booking_seats bs ON bs.booking_id = b.booking_id
                    WHERE b.user_id = :userId
                    """);
            appendFilters(sql, query);
            sql.append('\n');
            sql.append("""
                    GROUP BY b.booking_id, b.status, m.title, c.cinema_name, r.room_name,
                             s.starts_at, b.total_amount, b.currency, b.created_at
                    ORDER BY b.created_at
                    """);
            sql.append(query.sortDirection().name());
            sql.append(", b.booking_id ").append(query.sortDirection().name());
            sql.append(" LIMIT :limit OFFSET :offset");

            Query nativeQuery = entityManager.createNativeQuery(sql.toString());
            bindFilters(nativeQuery, userId, query);
            nativeQuery.setParameter("limit", query.size());
            nativeQuery.setParameter("offset", query.offset());

            @SuppressWarnings("unchecked")
            List<Object[]> rows = nativeQuery.getResultList();
            return rows.stream().map(this::mapRow).toList();
        } finally {
            entityManager.close();
        }
    }

    private void appendFilters(StringBuilder sql, UserBookingCriteria query) {
        if (query.status() != null) {
            sql.append(" AND b.status = :status");
        }
        if (query.fromInclusive() != null) {
            sql.append(" AND b.created_at >= :fromInclusive");
        }
        if (query.toExclusive() != null) {
            sql.append(" AND b.created_at < :toExclusive");
        }
    }

    private void bindFilters(Query nativeQuery, long userId, UserBookingCriteria query) {
        nativeQuery.setParameter("userId", userId);
        if (query.status() != null) {
            nativeQuery.setParameter("status", query.status());
        }
        if (query.fromInclusive() != null) {
            nativeQuery.setParameter("fromInclusive", query.fromInclusive());
        }
        if (query.toExclusive() != null) {
            nativeQuery.setParameter("toExclusive", query.toExclusive());
        }
    }

    private BookingRow mapRow(Object[] row) {
        return new BookingRow(
                ((Number) row[0]).longValue(),
                String.valueOf(row[1]),
                String.valueOf(row[2]),
                String.valueOf(row[3]),
                String.valueOf(row[4]),
                toInstant(row[5]),
                parseSeatLabels(String.valueOf(row[6])),
                ((Number) row[7]).longValue(),
                String.valueOf(row[8]).trim(),
                toInstant(row[9])
        );
    }

    private List<String> parseSeatLabels(String value) {
        try {
            return JSON.readValue(value, new TypeReference<>() { });
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể đọc danh sách ghế của booking", exception);
        }
    }

    static Instant toInstant(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime.toInstant();
        if (value instanceof ZonedDateTime zonedDateTime) return zonedDateTime.toInstant();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof LocalDateTime localDateTime) return localDateTime.toInstant(ZoneOffset.UTC);
        throw new IllegalStateException("Kiểu thời gian không được hỗ trợ: " + value);
    }

    public record BookingRow(
            long id,
            String status,
            String movieTitle,
            String cinemaName,
            String roomName,
            Instant startsAt,
            List<String> seatLabels,
            long totalAmount,
            String currency,
            Instant createdAt
    ) {
    }
}
