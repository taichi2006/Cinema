package com.cinema.showtime;

import com.cinema.cinema.Cinema;
import com.cinema.cinema.Room;
import com.cinema.common.util.JPAUtil;
import com.cinema.movie.Movie;
import com.cinema.showtime.dto.response.SeatResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ShowtimeDAO {

    public record ShowtimeInfo(
            Long showtimeId, Long movieId, String movieTitle, Long cinemaId, String cinemaName,
            Long roomId, String roomName, String screenPosition, Instant startsAtInstant,
            String startsAt, String endsAt, String format, String language, String status,
            Long minTicketPrice, String currency, Integer availableSeatCount
    ) {}

    public Optional<ShowtimeInfo> findShowtimeInfo(Long showtimeId) {
        if (showtimeId == null) return Optional.empty();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql =
                    "SELECT s, m, r, c, " +
                    "(SELECT COUNT(ss) FROM Seat ss WHERE ss.roomId = r.roomId AND ss.status = 'ACTIVE') " +
                    "FROM Showtime s, Room r, Cinema c, Movie m " +
                    "WHERE s.roomId = r.roomId AND r.cinemaId = c.cinemaId AND s.movieId = m.movieId " +
                    "AND s.showTimeId = :showtimeId";

            TypedQuery<Object[]> query = em.createQuery(jpql, Object[].class);
            query.setParameter("showtimeId", showtimeId.intValue());
            List<Object[]> results = query.getResultList();
            if (results.isEmpty()) return Optional.empty();

            Object[] row = results.get(0);
            Showtime s = (Showtime) row[0];
            Movie m = (Movie) row[1];
            Room r = (Room) row[2];
            Cinema c = (Cinema) row[3];
            Long availableSeats = (Long) row[4];

            Instant startsAt = (s.getShowDate() != null && s.getStartTime() != null)
                    ? LocalDateTime.of(s.getShowDate(), s.getStartTime()).atZone(ZoneId.systemDefault()).toInstant() : null;
            Instant endsAt = (s.getShowDate() != null && s.getEndTime() != null)
                    ? LocalDateTime.of(s.getShowDate(), s.getEndTime()).atZone(ZoneId.systemDefault()).toInstant() : null;

            return Optional.of(new ShowtimeInfo(
                    Long.valueOf(s.getShowTimeId()), Long.valueOf(s.getMovieId()), m.getTitle(),
                    c.getCinemaId(), c.getCinemaName(), Long.valueOf(r.getRoomId()), r.getRoomName(),
                    r.getScreenPosition() != null ? r.getScreenPosition() : "TOP",
                    startsAt, startsAt != null ? startsAt.toString() : null, endsAt != null ? endsAt.toString() : null,
                    m.getFormat(), m.getLanguage(), s.getStatus(),
                    s.getBasePrice() != null ? s.getBasePrice().longValue() : 0L, "VND",
                    availableSeats != null ? availableSeats.intValue() : 0
            ));
        } finally {
            em.close();
        }
    }

    public List<SeatResponse> findSeatsForShowtime(Long showtimeId, Long roomId, Long currentUserId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Showtime showtime = em.find(Showtime.class, showtimeId.intValue());

            List<Seat> seats = em.createQuery(
                    "SELECT se FROM Seat se WHERE se.roomId = :roomId ORDER BY se.seatRow ASC, se.seatCol ASC, se.seatId ASC",
                    Seat.class
            ).setParameter("roomId", roomId.intValue()).getResultList();

            List<ShowtimeSeat> showtimeSeats = em.createQuery(
                    "SELECT sts FROM ShowtimeSeat sts WHERE sts.showTimeId = :showtimeId",
                    ShowtimeSeat.class
            ).setParameter("showtimeId", showtimeId.intValue()).getResultList();

            Map<Integer, ShowtimeSeat> stsMap = new HashMap<>();
            for (ShowtimeSeat sts : showtimeSeats) stsMap.put(sts.getSeatId(), sts);

            LocalDateTime now = LocalDateTime.now();
            List<SeatResponse> responseList = new ArrayList<>();

            for (Seat se : seats) {
                ShowtimeSeat sts = stsMap.get(se.getSeatId());
                Long price = (sts != null && sts.getPrice() != null)
                        ? sts.getPrice().longValue()
                        : (showtime != null && showtime.getBasePrice() != null ? showtime.getBasePrice().longValue() : 0L);

                String seatStatus = "AVAILABLE";
                if (!"ACTIVE".equalsIgnoreCase(se.getStatus())) {
                    seatStatus = "BLOCKED";
                } else if (sts != null) {
                    if ("BOOKED".equalsIgnoreCase(sts.getStatus())) {
                        seatStatus = "BOOKED";
                    } else if ("SELECTED".equalsIgnoreCase(sts.getStatus())
                            && (sts.getHoldExpirationAt() == null || sts.getHoldExpirationAt().isAfter(now))) {
                        seatStatus = "HELD";
                    }
                }

                int y = (se.getSeatRow() != null && !se.getSeatRow().isEmpty())
                        ? (int) Character.toUpperCase(se.getSeatRow().charAt(0)) - 64 : 0;
                String holdExpiresAt = (sts != null && sts.getHoldExpirationAt() != null)
                        ? sts.getHoldExpirationAt().atZone(ZoneId.systemDefault()).toInstant().toString() : null;

                responseList.add(new SeatResponse(
                        String.valueOf(se.getSeatId()), se.getSeatRow(), se.getSeatCol(), se.getSeatCol(),
                        y, "REGULAR", price, seatStatus, false, holdExpiresAt
                ));
            }
            return responseList;
        } finally {
            em.close();
        }
    }
}
