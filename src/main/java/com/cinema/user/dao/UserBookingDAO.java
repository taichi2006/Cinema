package com.cinema.user.dao;

import com.cinema.booking.entity.Booking;
import com.cinema.common.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class UserBookingDAO {

    public record BookingRow(long bookingId, String status, long totalAmount) {}

    public long count(long userId, String status, Instant from, Instant to) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT count(b) FROM Booking b WHERE b.userId = :userId");
            if (status != null && !status.isBlank()) jpql.append(" AND b.status = :st");
            if (from != null) jpql.append(" AND b.createdAt >= :fromDt");
            if (to != null) jpql.append(" AND b.createdAt <= :toDt");

            var q = em.createQuery(jpql.toString(), Long.class).setParameter("userId", userId);
            if (status != null && !status.isBlank()) q.setParameter("st", status.trim());
            if (from != null) q.setParameter("fromDt", LocalDateTime.ofInstant(from, ZoneId.of("Asia/Ho_Chi_Minh")));
            if (to != null) q.setParameter("toDt", LocalDateTime.ofInstant(to, ZoneId.of("Asia/Ho_Chi_Minh")));

            return q.getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<BookingRow> findPage(long userId, String status, Instant from, Instant to,
                                     int page, int size, String sortDirection) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String order = "ASC".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
            StringBuilder jpql = new StringBuilder("SELECT b FROM Booking b WHERE b.userId = :userId");
            if (status != null && !status.isBlank()) jpql.append(" AND b.status = :st");
            if (from != null) jpql.append(" AND b.createdAt >= :fromDt");
            if (to != null) jpql.append(" AND b.createdAt <= :toDt");
            jpql.append(" ORDER BY b.createdAt ").append(order);

            TypedQuery<Booking> query = em.createQuery(jpql.toString(), Booking.class)
                    .setParameter("userId", userId)
                    .setFirstResult(page * size)
                    .setMaxResults(size);

            if (status != null && !status.isBlank()) query.setParameter("st", status.trim());
            if (from != null) query.setParameter("fromDt", LocalDateTime.ofInstant(from, ZoneId.of("Asia/Ho_Chi_Minh")));
            if (to != null) query.setParameter("toDt", LocalDateTime.ofInstant(to, ZoneId.of("Asia/Ho_Chi_Minh")));

            return query.getResultList().stream().map(b -> new BookingRow(
                    b.getBookingId(),
                    b.getStatus(),
                    b.getTotalAmount() != null ? b.getTotalAmount().longValue() : 0L
            )).toList();
        } finally {
            em.close();
        }
    }
}
