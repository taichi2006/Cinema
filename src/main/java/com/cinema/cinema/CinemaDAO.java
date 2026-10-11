package com.cinema.cinema;

import com.cinema.cinema.dto.request.CinemaRequest;
import com.cinema.cinema.dto.response.CinemaShowtimeResponse;
import com.cinema.common.util.JPAUtil;
import com.cinema.movie.Movie;
import com.cinema.showtime.Showtime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CinemaDAO {

    public Optional<Cinema> findById(Long cinemaId) {
        if (cinemaId == null) return Optional.empty();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Cinema.class, cinemaId));
        } finally {
            em.close();
        }
    }

    public List<Cinema> findCinemas(CinemaRequest filter) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT c FROM Cinema c WHERE c.status = 'ACTIVE' ");
            Map<String, Object> params = new HashMap<>();

            if (filter.getCity() != null && !filter.getCity().trim().isEmpty()) {
                jpql.append("AND LOWER(c.cityCode) = :city ");
                params.put("city", filter.getCity().trim().toLowerCase());
            }
            if (filter.getQ() != null && !filter.getQ().trim().isEmpty()) {
                jpql.append("AND (LOWER(c.cinemaName) LIKE :q OR LOWER(c.address) LIKE :q) ");
                params.put("q", "%" + filter.getQ().trim().toLowerCase() + "%");
            }

            jpql.append("name,desc".equalsIgnoreCase(filter.getSort())
                    ? "ORDER BY c.cinemaName DESC, c.cinemaId ASC"
                    : "ORDER BY c.cinemaName ASC, c.cinemaId ASC");

            TypedQuery<Cinema> query = em.createQuery(jpql.toString(), Cinema.class);
            params.forEach(query::setParameter);
            query.setFirstResult(Math.max(0, filter.getPage()) * Math.max(1, filter.getSize()));
            query.setMaxResults(filter.getSize());
            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public long countCinemas(CinemaRequest filter) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT COUNT(c) FROM Cinema c WHERE c.status = 'ACTIVE' ");
            Map<String, Object> params = new HashMap<>();

            if (filter.getCity() != null && !filter.getCity().trim().isEmpty()) {
                jpql.append("AND LOWER(c.cityCode) = :city ");
                params.put("city", filter.getCity().trim().toLowerCase());
            }
            if (filter.getQ() != null && !filter.getQ().trim().isEmpty()) {
                jpql.append("AND (LOWER(c.cinemaName) LIKE :q OR LOWER(c.address) LIKE :q) ");
                params.put("q", "%" + filter.getQ().trim().toLowerCase() + "%");
            }

            TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
            params.forEach(query::setParameter);
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<CinemaShowtimeResponse> findShowtimes(Long cinemaId, LocalDate date, Long movieId, int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT s, m, r, c, " +
                    "(SELECT COUNT(ss) FROM Seat ss WHERE ss.roomId = r.roomId AND ss.status = 'ACTIVE') " +
                    "FROM Showtime s, Room r, Cinema c, Movie m " +
                    "WHERE s.roomId = r.roomId AND r.cinemaId = c.cinemaId AND s.movieId = m.movieId " +
                    "AND c.cinemaId = :cinemaId AND c.status = 'ACTIVE' AND s.status IN ('SCHEDULED', 'ONGOING') " +
                    "AND s.showDate = :showDate "
            );
            if (movieId != null) jpql.append("AND s.movieId = :movieId ");
            jpql.append("ORDER BY s.startTime ASC, s.showTimeId ASC");

            TypedQuery<Object[]> query = em.createQuery(jpql.toString(), Object[].class)
                    .setParameter("cinemaId", cinemaId).setParameter("showDate", date);
            if (movieId != null) query.setParameter("movieId", movieId.intValue());
            query.setFirstResult(Math.max(0, page) * Math.max(1, size)).setMaxResults(size);

            List<CinemaShowtimeResponse> responses = new ArrayList<>();
            for (Object[] row : query.getResultList()) {
                Showtime s = (Showtime) row[0];
                Movie m = (Movie) row[1];
                Room r = (Room) row[2];
                Cinema c = (Cinema) row[3];
                Long availableSeats = (Long) row[4];

                responses.add(new CinemaShowtimeResponse(
                        String.valueOf(s.getShowTimeId()), String.valueOf(s.getMovieId()), m.getTitle(),
                        String.valueOf(c.getCinemaId()), c.getCinemaName(),
                        String.valueOf(r.getRoomId()), r.getRoomName(),
                        formatDateTime(s.getShowDate(), s.getStartTime()),
                        formatDateTime(s.getShowDate(), s.getEndTime()),
                        m.getFormat(), m.getLanguage(),
                        s.getBasePrice() != null ? s.getBasePrice().longValue() : 0L,
                        "VND", availableSeats != null ? availableSeats.intValue() : 0
                ));
            }
            return responses;
        } finally {
            em.close();
        }
    }

    public long countShowtimes(Long cinemaId, LocalDate date, Long movieId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT COUNT(s) FROM Showtime s, Room r, Cinema c " +
                    "WHERE s.roomId = r.roomId AND r.cinemaId = c.cinemaId " +
                    "AND c.cinemaId = :cinemaId AND c.status = 'ACTIVE' " +
                    "AND s.status IN ('SCHEDULED', 'ONGOING') AND s.showDate = :showDate "
            );
            if (movieId != null) jpql.append("AND s.movieId = :movieId ");

            TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class)
                    .setParameter("cinemaId", cinemaId).setParameter("showDate", date);
            if (movieId != null) query.setParameter("movieId", movieId.intValue());
            return query.getSingleResult();
        } finally {
            em.close();
        }
    }

    public record CityItem(String cityCode, String cityName) {}

    public List<CityItem> findDistinctCities() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<String> cities = em.createQuery(
                    "SELECT DISTINCT c.cityCode FROM Cinema c WHERE c.status = 'ACTIVE' ORDER BY c.cityCode ASC",
                    String.class
            ).getResultList();
            return cities.stream().map(c -> new CityItem(c, c)).toList();
        } finally {
            em.close();
        }
    }

    private String formatDateTime(LocalDate date, LocalTime time) {
        if (date == null || time == null) return null;
        return LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toString();
    }
}
