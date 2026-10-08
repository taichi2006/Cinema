package com.cinema.showtime.dao;

import com.cinema.common.util.JPAUtil;
import com.cinema.showtime.dto.response.SeatResponse;
import com.cinema.showtime.dto.response.ShowtimeResponse;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
            Integer availableSeatCount) {
    }

    public Optional<ShowtimeInfo> findShowtimeInfo(Long showtimeId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            String sql = "SELECT " +
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
                    "    COALESCE((SELECT MIN(ss.price) FROM cinema.showtime_seats ss WHERE ss.showtime_id = s.showtime_id AND ss.is_blocked = false), s.base_price) AS min_ticket_price, "
                    +
                    "    'VND' AS currency, " +
                    "    CASE " +
                    "        WHEN (SELECT COUNT(*) FROM cinema.showtime_seats ss WHERE ss.showtime_id = s.showtime_id) > 0 THEN "
                    +
                    "            (SELECT COUNT(ss.seat_id) FROM cinema.showtime_seats ss WHERE ss.showtime_id = s.showtime_id AND ss.is_blocked = false AND NOT EXISTS (SELECT 1 FROM cinema.booking_seats bs WHERE bs.showtime_id = s.showtime_id AND bs.seat_id = ss.seat_id AND bs.status IN ('HELD', 'BOOKED'))) "
                    +
                    "        ELSE " +
                    "            GREATEST(0, r.capacity - COALESCE((SELECT COUNT(bs.seat_id) FROM cinema.booking_seats bs WHERE bs.showtime_id = s.showtime_id AND bs.status IN ('HELD', 'BOOKED')), 0)) "
                    +
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
                    availableSeatCount));
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

    public Optional<ShowtimeResponse> findShowtimeById(Long showTimeId) {
        if (showTimeId == null) {
            return Optional.empty();
        }
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            List<?> results;
            try {
                String sql = "SELECT s.show_time_id, s.movie_id, s.room_id, " +
                        "CAST(s.show_date AS TEXT), CAST(s.start_time AS TEXT), CAST(s.end_time AS TEXT), " +
                        "s.base_price, s.status " +
                        "FROM cinema.show_times s WHERE s.show_time_id = :showTimeId";
                var query = entityManager.createNativeQuery(sql);
                query.setParameter("showTimeId", showTimeId);
                results = query.getResultList();
            } catch (Exception e) {
                String fallbackSql = "SELECT s.showtime_id, s.movie_id, s.room_id, " +
                        "CAST(s.starts_at AS DATE), CAST(s.starts_at AS TIME), CAST(s.ends_at AS TIME), " +
                        "s.base_price, s.status " +
                        "FROM cinema.showtimes s WHERE s.showtime_id = :showTimeId";
                var fallbackQuery = entityManager.createNativeQuery(fallbackSql);
                fallbackQuery.setParameter("showTimeId", showTimeId);
                results = fallbackQuery.getResultList();
            }

            if (results.isEmpty()) {
                return Optional.empty();
            }

            Object[] row = (Object[]) results.get(0);
            Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
            Long movieId = row[1] != null ? ((Number) row[1]).longValue() : null;
            Long roomId = row[2] != null ? ((Number) row[2]).longValue() : null;
            String showDate = row[3] != null ? row[3].toString() : null;
            String startTime = row[4] != null ? row[4].toString() : null;
            String endTime = row[5] != null ? row[5].toString() : null;
            Double basePrice = row[6] != null ? ((Number) row[6]).doubleValue() : null;
            String status = row[7] != null ? row[7].toString() : null;

            return Optional.of(new ShowtimeResponse(
                    id,
                    movieId,
                    roomId,
                    showDate,
                    startTime,
                    endTime,
                    basePrice,
                    status));
        } finally {
            entityManager.close();
        }
    }

    public List<SeatResponse> findSeatsByShowtimeId(Long showTimeId) {
        if (showTimeId == null) {
            return Collections.emptyList();
        }
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            List<?> results = Collections.emptyList();
            try {
                String sql = "SELECT " +
                        "    sts.id, " +
                        "    sts.seat_id, " +
                        "    s.seat_row, " +
                        "    s.seat_col, " +
                        "    s.seat_label, " +
                        "    sts.price, " +
                        "    CASE " +
                        "        WHEN sts.status = 'SELECTED' AND sts.hold_expiration_at IS NOT NULL AND sts.hold_expiration_at < CURRENT_TIMESTAMP THEN 'AVAILABLE' "
                        +
                        "        ELSE sts.status " +
                        "    END AS actual_status, " +
                        "    CASE " +
                        "        WHEN sts.status = 'SELECTED' AND sts.hold_expiration_at IS NOT NULL AND sts.hold_expiration_at < CURRENT_TIMESTAMP THEN NULL "
                        +
                        "        ELSE CAST(sts.hold_expiration_at AS TEXT) " +
                        "    END AS actual_hold_expiration_at " +
                        "FROM cinema.show_time_seats sts " +
                        "JOIN cinema.seats s ON sts.seat_id = s.seat_id " +
                        "WHERE sts.show_time_id = :showTimeId " +
                        "ORDER BY s.seat_row ASC, s.seat_col ASC";
                var query = entityManager.createNativeQuery(sql);
                query.setParameter("showTimeId", showTimeId);
                results = query.getResultList();
            } catch (Exception e) {
                try {
                    String fallbackSql = "SELECT " +
                            "    sts.id, " +
                            "    sts.seat_id, " +
                            "    s.seat_row, " +
                            "    s.seat_number, " +
                            "    CONCAT(s.seat_row, s.seat_number), " +
                            "    sts.price, " +
                            "    'AVAILABLE', " +
                            "    NULL " +
                            "FROM cinema.showtime_seats sts " +
                            "JOIN cinema.seats s ON sts.seat_id = s.seat_id " +
                            "WHERE sts.showtime_id = :showTimeId " +
                            "ORDER BY s.seat_row ASC, s.seat_number ASC";
                    var fallbackQuery = entityManager.createNativeQuery(fallbackSql);
                    fallbackQuery.setParameter("showTimeId", showTimeId);
                    results = fallbackQuery.getResultList();
                } catch (Exception ex) {
                    results = Collections.emptyList();
                }
            }

            List<SeatResponse> seats = new ArrayList<>();
            for (Object rowObj : results) {
                Object[] row = (Object[]) rowObj;
                Long id = row[0] != null ? ((Number) row[0]).longValue() : null;
                Long seatId = row[1] != null ? ((Number) row[1]).longValue() : null;
                String seatRow = row[2] != null ? row[2].toString() : null;
                Integer seatCol = row[3] != null ? ((Number) row[3]).intValue() : null;
                String seatLabel = row[4] != null ? row[4].toString()
                        : (seatRow != null && seatCol != null ? seatRow + seatCol : null);
                Double price = row[5] != null ? ((Number) row[5]).doubleValue() : null;
                String status = row[6] != null ? row[6].toString() : "AVAILABLE";
                String holdExpirationAt = row[7] != null ? formatTimestamp(row[7]) : null;

                seats.add(new SeatResponse(
                        id,
                        seatId,
                        seatRow,
                        seatCol,
                        seatLabel,
                        price,
                        status,
                        holdExpirationAt));
            }
            return seats;
        } finally {
            entityManager.close();
        }
    }
}
