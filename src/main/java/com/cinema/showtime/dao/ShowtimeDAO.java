package com.cinema.showtime;

import com.cinema.common.util.JPAUtil;
import com.cinema.showtime.DTO.Response.SeatResponse;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShowtimeDAO {

    public record ShowtimeInfo(
            Long showtimeId,
            Long movieId,
            String movieTitle,
            Long cinemaId,
            String cinemaName,
            Long roomId,
            String roomName,
            String screenPosition,
            Instant startsAtInstant,
            String startsAt,
            String endsAt,
            String format,
            String language,
            String status,
            Long minTicketPrice,
            String currency,
            Integer availableSeatCount
    ) {}

    public Optional<ShowtimeInfo> findShowtimeInfo(Long showtimeId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            String sql =
                    "SELECT " +
                    "    s.showtime_id, " +
                    "    s.movie_id, " +
                    "    m.title AS movie_title, " +
                    "    c.cinema_id, " +
                    "    c.cinema_name, " +
                    "    r.room_id, " +
                    "    r.room_name, " +
                    "    r.screen_position, " +
                    "    s.starts_at, " +
                    "    s.ends_at, " +
                    "    s.format, " +
                    "    s.language, " +
                    "    s.status, " +
                    "    COALESCE((SELECT MIN(ss.price) FROM cinema.showtime_seats ss WHERE ss.showtime_id = s.showtime_id AND ss.is_blocked = false), s.base_price) AS min_ticket_price, " +
                    "    'VND' AS currency, " +
                    "    CASE " +
                    "        WHEN (SELECT COUNT(*) FROM cinema.showtime_seats ss WHERE ss.showtime_id = s.showtime_id) > 0 THEN " +
                    "            (SELECT COUNT(ss.seat_id) FROM cinema.showtime_seats ss WHERE ss.showtime_id = s.showtime_id AND ss.is_blocked = false AND NOT EXISTS (SELECT 1 FROM cinema.booking_seats bs WHERE bs.showtime_id = s.showtime_id AND bs.seat_id = ss.seat_id AND bs.status IN ('HELD', 'BOOKED'))) " +
                    "        ELSE " +
                    "            GREATEST(0, r.capacity - COALESCE((SELECT COUNT(bs.seat_id) FROM cinema.booking_seats bs WHERE bs.showtime_id = s.showtime_id AND bs.status IN ('HELD', 'BOOKED')), 0)) " +
                    "    END AS available_seat_count " +
                    "FROM cinema.showtimes s " +
                    "JOIN cinema.rooms r ON s.room_id = r.room_id " +
                    "JOIN cinema.cinemas c ON r.cinema_id = c.cinema_id " +
                    "JOIN cinema.movies m ON s.movie_id = m.movie_id " +
                    "WHERE s.showtime_id = :showtimeId";

            var query = entityManager.createNativeQuery(sql);
            query.setParameter("showtimeId", showtimeId);

            List<?> results = query.getResultList();
            if (results.isEmpty()) {
                return Optional.empty();
            }

            Object[] row = (Object[]) results.get(0);
            Long sId = row[0] != null ? ((Number) row[0]).longValue() : null;
            Long movieId = row[1] != null ? ((Number) row[1]).longValue() : null;
            String movieTitle = row[2] != null ? row[2].toString() : null;
            Long cinemaId = row[3] != null ? ((Number) row[3]).longValue() : null;
            String cinemaName = row[4] != null ? row[4].toString() : null;
            Long roomId = row[5] != null ? ((Number) row[5]).longValue() : null;
            String roomName = row[6] != null ? row[6].toString() : null;
            String screenPosition = row[7] != null ? row[7].toString() : "TOP";
            Instant startsAtInstant = toInstant(row[8]);
            String startsAt = formatTimestamp(row[8]);
            String endsAt = formatTimestamp(row[9]);
            String format = row[10] != null ? row[10].toString() : null;
            String language = row[11] != null ? row[11].toString() : null;
            String status = row[12] != null ? row[12].toString() : null;
            Long minTicketPrice = row[13] != null ? ((Number) row[13]).longValue() : null;
            String currency = row[14] != null ? row[14].toString() : "VND";
            Integer availableSeatCount = row[15] != null ? ((Number) row[15]).intValue() : null;

            return Optional.of(new ShowtimeInfo(
                    sId,
                    movieId,
                    movieTitle,
                    cinemaId,
                    cinemaName,
                    roomId,
                    roomName,
                    screenPosition,
                    startsAtInstant,
                    startsAt,
                    endsAt,
                    format,
                    language,
                    status,
                    minTicketPrice,
                    currency,
                    availableSeatCount
            ));
        } finally {
            entityManager.close();
        }
    }

    public List<SeatResponse> findSeatsForShowtime(Long showtimeId, Long roomId, Long currentUserId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            String sql =
                    "SELECT " +
                    "    se.seat_id, " +
                    "    se.seat_row, " +
                    "    se.seat_number, " +
                    "    se.x, " +
                    "    se.y, " +
                    "    st.type_name, " +
                    "    COALESCE(ss.price, s.base_price + st.extra_price) AS price, " +
                    "    CASE " +
                    "        WHEN se.status = 'BLOCKED' OR COALESCE(ss.is_blocked, false) = true THEN 'BLOCKED' " +
                    "        WHEN bs.status = 'BOOKED' OR b.status = 'PAID' THEN 'BOOKED' " +
                    "        WHEN bs.status = 'HELD' AND b.status = 'PENDING_PAYMENT' AND b.expires_at > CURRENT_TIMESTAMP THEN 'HELD' " +
                    "        ELSE 'AVAILABLE' " +
                    "    END AS seat_status, " +
                    "    CASE " +
                    "        WHEN bs.status = 'HELD' AND b.status = 'PENDING_PAYMENT' AND b.expires_at > CURRENT_TIMESTAMP AND b.user_id = :currentUserId THEN true " +
                    "        ELSE false " +
                    "    END AS held_by_current_user, " +
                    "    CASE " +
                    "        WHEN bs.status = 'HELD' AND b.status = 'PENDING_PAYMENT' AND b.expires_at > CURRENT_TIMESTAMP THEN b.expires_at " +
                    "        ELSE NULL " +
                    "    END AS hold_expires_at " +
                    "FROM cinema.seats se " +
                    "JOIN cinema.rooms r ON se.room_id = r.room_id " +
                    "JOIN cinema.seat_types st ON se.seat_type_id = st.seat_type_id " +
                    "JOIN cinema.showtimes s ON s.showtime_id = :showtimeId AND s.room_id = r.room_id " +
                    "LEFT JOIN cinema.showtime_seats ss ON ss.showtime_id = s.showtime_id AND ss.seat_id = se.seat_id " +
                    "LEFT JOIN cinema.booking_seats bs ON bs.showtime_id = s.showtime_id AND bs.seat_id = se.seat_id AND bs.status IN ('HELD', 'BOOKED') " +
                    "LEFT JOIN cinema.bookings b ON bs.booking_id = b.booking_id AND b.status IN ('PENDING_PAYMENT', 'PAID') " +
                    "WHERE se.room_id = :roomId " +
                    "ORDER BY se.y ASC, se.x ASC, se.seat_id ASC";

            var query = entityManager.createNativeQuery(sql);
            query.setParameter("showtimeId", showtimeId);
            query.setParameter("roomId", roomId);
            query.setParameter("currentUserId", currentUserId != null ? currentUserId : -1L);

            List<?> results = query.getResultList();
            List<SeatResponse> seats = new ArrayList<>();

            for (Object rowObj : results) {
                Object[] row = (Object[]) rowObj;
                String seatId = row[0] != null ? row[0].toString() : null;
                String rowName = row[1] != null ? row[1].toString() : null;
                Integer number = row[2] != null ? ((Number) row[2]).intValue() : null;
                Integer x = row[3] != null ? ((Number) row[3]).intValue() : null;
                Integer y = row[4] != null ? ((Number) row[4]).intValue() : null;
                String type = row[5] != null ? row[5].toString() : null;
                Long price = row[6] != null ? ((Number) row[6]).longValue() : 0L;
                String status = row[7] != null ? row[7].toString() : "AVAILABLE";
                Boolean heldByCurrentUser = row[8] != null ? Boolean.parseBoolean(row[8].toString()) : false;
                String holdExpiresAt = formatTimestamp(row[9]);

                seats.add(new SeatResponse(
                        seatId,
                        rowName,
                        number,
                        x,
                        y,
                        type,
                        price,
                        status,
                        heldByCurrentUser,
                        holdExpiresAt
                ));
            }

            return seats;
        } finally {
            entityManager.close();
        }
    }

    public static String formatTimestamp(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof java.time.OffsetDateTime odt) {
            return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        if (obj instanceof java.time.Instant inst) {
            return inst.toString();
        }
        if (obj instanceof java.time.ZonedDateTime zdt) {
            return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        if (obj instanceof java.sql.Timestamp ts) {
            return ts.toInstant().toString();
        }
        return obj.toString();
    }

    public static Instant toInstant(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof java.time.Instant inst) {
            return inst;
        }
        if (obj instanceof java.time.OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (obj instanceof java.time.ZonedDateTime zdt) {
            return zdt.toInstant();
        }
        if (obj instanceof java.sql.Timestamp ts) {
            return ts.toInstant();
        }
        try {
            return Instant.parse(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
