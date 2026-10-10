package com.cinema.ticket;

import com.cinema.common.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

public class TicketDAO {

    //Lấy danh sách vé theo bookingId.
    public List<Ticket> findByBookingId(Long bookingId) {
        if (bookingId == null) return List.of();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Ticket> query = em.createQuery(
                    "SELECT t FROM Ticket t WHERE t.bookingId = :bookingId ORDER BY t.ticketId ASC",
                    Ticket.class
            );
            query.setParameter("bookingId", bookingId);
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    // Tìm vé theo ticketId.
    public Optional<Ticket> findById(Long ticketId) {
        if (ticketId == null) return Optional.empty();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Ticket.class, ticketId));
        } finally {
            em.close();
        }
    }

    // Lấy user_id sở hữu đơn booking_id tương ứng qua Native Query
    public Optional<Long> findUserIdByBookingId(Long bookingId) {
        if (bookingId == null) return Optional.empty();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Object result = em.createNativeQuery(
                    "SELECT user_id FROM cinema.bookings WHERE booking_id = :bookingId"
            ).setParameter("bookingId", bookingId).getSingleResult();

            if (result instanceof Number n) {
                return Optional.of(n.longValue());
            }
            return Optional.empty();
        } catch (NoResultException e) {
            return Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        } finally {
            em.close();
        }
    }
}
