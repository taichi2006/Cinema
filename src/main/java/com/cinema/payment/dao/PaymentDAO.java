package com.cinema.payment.dao;

import com.cinema.booking.entity.Booking;
import com.cinema.common.util.JPAUtil;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class PaymentDAO {

    public Optional<Long> findBookingOwnerId(long bookingId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT b.userId FROM Booking b WHERE b.bookingId = :bookingId", Long.class)
                    .setParameter("bookingId", bookingId)
                    .getResultStream()
                    .findFirst();
        } finally {
            em.close();
        }
    }

    public List<Payment> findByBookingId(long bookingId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                            "SELECT p FROM Payment p WHERE p.bookingId = :bookingId ORDER BY p.id DESC",
                            Payment.class)
                    .setParameter("bookingId", bookingId)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public Optional<Payment> findById(long paymentId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Payment.class, paymentId));
        } finally {
            em.close();
        }
    }

    public Optional<Payment> findByIdForUpdate(long paymentId, EntityManager em) {
        return Optional.ofNullable(em.find(Payment.class, paymentId, LockModeType.PESSIMISTIC_WRITE));
    }

    public boolean existsSuccessfulByBookingId(long bookingId, EntityManager em) {
        Long count = em.createQuery(
                        "SELECT count(p) FROM Payment p " +
                                "WHERE p.bookingId = :bookingId AND p.status = :status", Long.class)
                .setParameter("bookingId", bookingId)
                .setParameter("status", PaymentStatus.SUCCESSFUL)
                .getSingleResult();
        return count > 0;
    }

    public boolean existsActivePendingByBookingId(long bookingId, LocalDateTime now, EntityManager em) {
        Long count = em.createQuery(
                        "SELECT count(p) FROM Payment p WHERE p.bookingId = :bookingId " +
                                "AND p.status = :status " +
                                "AND (p.expiredAt IS NULL OR p.expiredAt > :now)", Long.class)
                .setParameter("bookingId", bookingId)
                .setParameter("status", PaymentStatus.PENDING)
                .setParameter("now", now)
                .getSingleResult();
        return count > 0;
    }

    public void cancelPendingPaymentsByBookingId(long bookingId, String reason, EntityManager em) {
        List<Payment> pendingList = em.createQuery(
                        "SELECT p FROM Payment p WHERE p.bookingId = :bookingId AND p.status = :status", Payment.class)
                .setParameter("bookingId", bookingId)
                .setParameter("status", PaymentStatus.PENDING)
                .getResultList();
        for (Payment p : pendingList) {
            p.setStatus(PaymentStatus.FAILED);
            p.setFailureReason(reason != null ? reason : "Hủy giao dịch chờ để tạo thanh toán mới");
        }
        if (!pendingList.isEmpty()) {
            em.flush();
        }
    }

    public Payment save(Payment payment, EntityManager em) {
        if (payment.getId() == null) {
            em.persist(payment);
            return payment;
        }
        return em.merge(payment);
    }
}
