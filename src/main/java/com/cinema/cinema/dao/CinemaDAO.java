package com.cinema.cinema;

import com.cinema.cinema.DTO.Request.CinemaRequest;
import com.cinema.cinema.DTO.Response.CinemaShowtimeResponse;
import com.cinema.common.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CinemaDAO {

    public Optional<Cinema> findById(Long cinemaId) {
        if (cinemaId == null) {
            return Optional.empty();
        }
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            Cinema cinema = entityManager.find(Cinema.class, cinemaId);
            return Optional.ofNullable(cinema);
        } finally {
            entityManager.close();
        }
    }

    public List<Cinema> findCinemas(CinemaRequest filter) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT c FROM Cinema c WHERE c.status = 'ACTIVE' ");
            Map<String, Object> params = new HashMap<>();

            if (filter.getCity() != null && !filter.getCity().trim().isEmpty()) {
                jpql.append("AND (LOWER(c.cityCode) = :city OR LOWER(c.cityName) = :city) ");
                params.put("city", filter.getCity().trim().toLowerCase());
            }

            if (filter.getQ() != null && !filter.getQ().trim().isEmpty()) {
                jpql.append("AND (LOWER(c.cinemaName) LIKE :q OR LOWER(c.address) LIKE :q) ");
                params.put("q", "%" + filter.getQ().trim().toLowerCase() + "%");
            }

            if ("name,desc".equalsIgnoreCase(filter.getSort())) {
                jpql.append("ORDER BY c.cinemaName DESC, c.cinemaId ASC");
            } else {
                jpql.append("ORDER BY c.cinemaName ASC, c.cinemaId ASC");
            }

            TypedQuery<Cinema> query = entityManager.createQuery(jpql.toString(), Cinema.class);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            int offset = Math.max(0, filter.getPage()) * Math.max(1, filter.getSize());
            query.setFirstResult(offset);
            query.setMaxResults(filter.getSize());

            return query.getResultList();
        } finally {
            entityManager.close();
        }
    }

    public long countCinemas(CinemaRequest filter) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT COUNT(c) FROM Cinema c WHERE c.status = 'ACTIVE' ");
            Map<String, Object> params = new HashMap<>();

            if (filter.getCity() != null && !filter.getCity().trim().isEmpty()) {
                jpql.append("AND (LOWER(c.cityCode) = :city OR LOWER(c.cityName) = :city) ");
                params.put("city", filter.getCity().trim().toLowerCase());
            }

            if (filter.getQ() != null && !filter.getQ().trim().isEmpty()) {
                jpql.append("AND (LOWER(c.cinemaName) LIKE :q OR LOWER(c.address) LIKE :q) ");
                params.put("q", "%" + filter.getQ().trim().toLowerCase() + "%");
            }

            TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            return query.getSingleResult();
        } finally {
            entityManager.close();
        }
    }

    public List<CinemaShowtimeResponse> findShowtimes(Long cinemaId, LocalDate date, Long movieId, int page, int size) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT " +
                    "s.showtime_id, " +
                    "s.movie_id, " +
                    "m.title, " +
                    "c.cinema_id, " +
                    "c.cinema_name, " +
                    "r.room_id, " +
                    "r.room_name, " +
                    "s.starts_at, " +
                    "s.ends_at, " +
                    "s.format, " +
                    "s.language, " +
                    "COALESCE((SELECT MIN(ss.price) FROM cinema.showtime_seats ss WHERE ss.showtime_id = s.showtime_id AND ss.is_blocked = false), s.base_price) AS min_ticket_price, " +
                    "'VND' AS currency, " +
                    "CASE " +
                    "    WHEN (SELECT COUNT(*) FROM cinema.showtime_seats ss WHERE ss.showtime_id = s.showtime_id) > 0 THEN " +
                    "        (SELECT COUNT(ss.seat_id) FROM cinema.showtime_seats ss WHERE ss.showtime_id = s.showtime_id AND ss.is_blocked = false AND NOT EXISTS (SELECT 1 FROM cinema.booking_seats bs WHERE bs.showtime_id = s.showtime_id AND bs.seat_id = ss.seat_id AND bs.status IN ('HELD', 'BOOKED'))) " +
                    "    ELSE " +
                    "        GREATEST(0, r.capacity - COALESCE((SELECT COUNT(bs.seat_id) FROM cinema.booking_seats bs WHERE bs.showtime_id = s.showtime_id AND bs.status IN ('HELD', 'BOOKED')), 0)) " +
                    "END AS available_seat_count " +
                    "FROM cinema.showtimes s " +
                    "JOIN cinema.rooms r ON s.room_id = r.room_id " +
                    "JOIN cinema.cinemas c ON r.cinema_id = c.cinema_id " +
                    "JOIN cinema.movies m ON s.movie_id = m.movie_id " +
                    "WHERE c.cinema_id = :cinemaId " +
                    "AND c.status = 'ACTIVE' " +
                    "AND s.status = 'OPEN' " +
                    "AND CAST(s.starts_at AS date) = CAST(:showDate AS date) "
            );

            if (movieId != null) {
                sql.append("AND s.movie_id = :movieId ");
            }

            sql.append("ORDER BY s.starts_at ASC, s.showtime_id ASC");

            var query = entityManager.createNativeQuery(sql.toString());
            query.setParameter("cinemaId", cinemaId);
            query.setParameter("showDate", date.toString());
            if (movieId != null) {
                query.setParameter("movieId", movieId);
            }

            int offset = Math.max(0, page) * Math.max(1, size);
            query.setFirstResult(offset);
            query.setMaxResults(size);

            List<?> rawResults = query.getResultList();
            List<CinemaShowtimeResponse> responses = new ArrayList<>();
            for (Object rowObj : rawResults) {
                Object[] row = (Object[]) rowObj;
                String showtimeId = row[0] != null ? row[0].toString() : null;
                String mId = row[1] != null ? row[1].toString() : null;
                String movieTitle = row[2] != null ? row[2].toString() : null;
                String cId = row[3] != null ? row[3].toString() : null;
                String cName = row[4] != null ? row[4].toString() : null;
                String rId = row[5] != null ? row[5].toString() : null;
                String rName = row[6] != null ? row[6].toString() : null;
                String startsAt = formatTimestamp(row[7]);
                String endsAt = formatTimestamp(row[8]);
                String format = row[9] != null ? row[9].toString() : null;
                String language = row[10] != null ? row[10].toString() : null;
                Long minTicketPrice = row[11] != null ? ((Number) row[11]).longValue() : null;
                String currency = row[12] != null ? row[12].toString() : "VND";
                Integer availableSeatCount = row[13] != null ? ((Number) row[13]).intValue() : null;

                responses.add(new CinemaShowtimeResponse(
                        showtimeId, mId, movieTitle, cId, cName, rId, rName, startsAt, endsAt, format, language, minTicketPrice, currency, availableSeatCount
                ));
            }
            return responses;
        } finally {
            entityManager.close();
        }
    }

    public long countShowtimes(Long cinemaId, LocalDate date, Long movieId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT COUNT(*) " +
                    "FROM cinema.showtimes s " +
                    "JOIN cinema.rooms r ON s.room_id = r.room_id " +
                    "JOIN cinema.cinemas c ON r.cinema_id = c.cinema_id " +
                    "WHERE c.cinema_id = :cinemaId " +
                    "AND c.status = 'ACTIVE' " +
                    "AND s.status = 'OPEN' " +
                    "AND CAST(s.starts_at AS date) = CAST(:showDate AS date) "
            );

            if (movieId != null) {
                sql.append("AND s.movie_id = :movieId ");
            }

            var query = entityManager.createNativeQuery(sql.toString());
            query.setParameter("cinemaId", cinemaId);
            query.setParameter("showDate", date.toString());
            if (movieId != null) {
                query.setParameter("movieId", movieId);
            }

            Object result = query.getSingleResult();
            return result != null ? ((Number) result).longValue() : 0L;
        } finally {
            entityManager.close();
        }
    }

    private String formatTimestamp(Object obj) {
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

    public record CityItem(String cityCode, String cityName) {}

    public List<CityItem> findDistinctCities() {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            String sql = "SELECT DISTINCT c.city_code, c.city_name FROM cinema.cinemas c WHERE c.status = 'ACTIVE' ORDER BY c.city_name ASC";
            var query = entityManager.createNativeQuery(sql);
            List<?> results = query.getResultList();
            List<CityItem> cities = new ArrayList<>();
            for (Object rowObj : results) {
                Object[] row = (Object[]) rowObj;
                String code = row[0] != null ? row[0].toString() : null;
                String name = row[1] != null ? row[1].toString() : null;
                cities.add(new CityItem(code, name));
            }
            return cities;
        } finally {
            entityManager.close();
        }
    }
}
