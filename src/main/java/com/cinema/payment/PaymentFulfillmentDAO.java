package com.cinema.payment;

import com.cinema.ticket.Ticket;
import com.cinema.ticket.TicketStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PaymentFulfillmentDAO {

    public List<Long> lockHeldSeatIds(long bookingId, EntityManager em) {
        Query query = em.createNativeQuery("""
                SELECT id
                FROM cinema.show_time_seats
                WHERE booking_id = :bookingId AND status = 'SELECTED'
                FOR UPDATE
                """);
        query.setParameter("bookingId", bookingId);

        List<?> rows = query.getResultList();
        List<Long> ids = new ArrayList<>(rows.size());
        for (Object row : rows) {
            ids.add(((Number) row).longValue());
        }
        return ids;
    }

    public List<TicketSnapshot> findTicketSnapshots(long bookingId, EntityManager em) {
        Query query = em.createNativeQuery("""
                SELECT sts.id,
                       sts.price,
                       m.title,
                       st.show_date,
                       st.start_time,
                       r.room_name,
                       COALESCE(s.seat_label, CONCAT(s.seat_row, s.seat_col))
                FROM cinema.show_time_seats sts
                JOIN cinema.show_times st ON st.show_time_id = sts.show_time_id
                JOIN cinema.movies m ON m.movie_id = st.movie_id
                JOIN cinema.rooms r ON r.room_id = st.room_id
                JOIN cinema.seats s ON s.seat_id = sts.seat_id
                WHERE sts.booking_id = :bookingId AND sts.status = 'SELECTED'
                ORDER BY sts.id
                """);
        query.setParameter("bookingId", bookingId);

        List<?> rows = query.getResultList();
        List<TicketSnapshot> snapshots = new ArrayList<>(rows.size());
        for (Object row : rows) {
            Object[] values = (Object[]) row;
            snapshots.add(new TicketSnapshot(
                    ((Number) values[0]).longValue(),
                    new BigDecimal(values[1].toString()),
                    (String) values[2],
                    toLocalDateTime(values[3], values[4]),
                    (String) values[5],
                    (String) values[6]
            ));
        }
        return snapshots;
    }

    public int markHeldSeatsBooked(long bookingId, EntityManager em) {
        Query query = em.createNativeQuery("""
                UPDATE cinema.show_time_seats
                SET status = 'BOOKED',
                    hold_expiration_at = NULL,
                    version = version + 1
                WHERE booking_id = :bookingId AND status = 'SELECTED'
                """);
        query.setParameter("bookingId", bookingId);
        return query.executeUpdate();
    }

    public void createTickets(long bookingId, List<TicketSnapshot> snapshots, EntityManager em) {
        for (TicketSnapshot snapshot : snapshots) {
            Ticket ticket = new Ticket(
                    bookingId,
                    snapshot.showTimeSeatId(),
                    "TKT-" + UUID.randomUUID(),
                    TicketStatus.VALID,
                    snapshot.movieName(),
                    snapshot.showTimeStartTime(),
                    snapshot.roomName(),
                    snapshot.seatName(),
                    snapshot.price()
            );
            em.persist(ticket);
        }
    }

    public void markUserVoucherUsed(long userId, long voucherId, LocalDateTime now, EntityManager em) {
        Query query = em.createNativeQuery("""
                UPDATE cinema.user_vouchers
                SET status = 'USED', used_at = :usedAt
                WHERE user_id = :userId AND voucher_id = :voucherId AND status = 'UNUSED'
                """);
        query.setParameter("usedAt", now);
        query.setParameter("userId", userId);
        query.setParameter("voucherId", voucherId);
        query.executeUpdate();
    }

    public int releaseHeldSeats(long bookingId, EntityManager em) {
        Query query = em.createNativeQuery("""
                UPDATE cinema.show_time_seats
                SET status = 'AVAILABLE', booking_id = NULL, hold_expiration_at = NULL
                WHERE booking_id = :bookingId AND status = 'SELECTED'
                """);
        query.setParameter("bookingId", bookingId);
        return query.executeUpdate();
    }

    private LocalDateTime toLocalDateTime(Object dateValue, Object timeValue) {
        LocalDate date = dateValue instanceof Date sqlDate ? sqlDate.toLocalDate() : (LocalDate) dateValue;
        LocalTime time = timeValue instanceof Time sqlTime ? sqlTime.toLocalTime() : (LocalTime) timeValue;
        return LocalDateTime.of(date, time);
    }

    public record TicketSnapshot(
            long showTimeSeatId,
            BigDecimal price,
            String movieName,
            LocalDateTime showTimeStartTime,
            String roomName,
            String seatName) {
    }
}
