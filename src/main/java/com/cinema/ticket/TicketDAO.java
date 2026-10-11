package com.cinema.ticket;

import com.cinema.common.util.JPAUtil;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

public class TicketDAO {

    // 1. Tìm vé theo ID
    public Optional<Ticket> findById(Long ticketId) {
        if (ticketId == null) return Optional.empty();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Ticket.class, ticketId));
        } finally {
            em.close();
        }
    }

    // 2. Danh sách vé theo bookingId
    public List<Ticket> findByBookingId(Long bookingId) {
        if (bookingId == null) return List.of();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT t FROM Ticket t WHERE t.bookingId = :bId ORDER BY t.ticketId ASC", Ticket.class)
                    .setParameter("bId", bookingId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // 3. Tìm chủ sở hữu (userId) của bookingId
    public Optional<Long> findUserIdByBookingId(Long bookingId) {
        if (bookingId == null) return Optional.empty();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Long> list = em.createQuery("SELECT b.userId FROM Booking b WHERE b.bookingId = :bId", Long.class)
                    .setParameter("bId", bookingId)
                    .getResultList();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } finally {
            em.close();
        }
    }
}
