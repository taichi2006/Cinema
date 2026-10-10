package com.cinema.cinema.dao;

import com.cinema.cinema.dto.request.CinemaRequest;
import com.cinema.cinema.entity.Cinema;
import com.cinema.common.util.JPAUtil;
import com.cinema.movie.dto.response.ShowtimeResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
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
            StringBuilder jpql = new StringBuilder("SELECT c FROM Cinema c WHERE 1=1 ");
            Map<String, Object> params = new HashMap<>();

            if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
                jpql.append("AND c.status = :status ");
                params.put("status", filter.getStatus().trim().toUpperCase());
            }

            if (filter.getCity() != null && !filter.getCity().trim().isEmpty()) {
                jpql.append("AND LOWER(c.city) LIKE :city ");
                params.put("city", "%" + filter.getCity().trim().toLowerCase() + "%");
            }

            jpql.append("ORDER BY c.cinemaId ASC");

            TypedQuery<Cinema> query = entityManager.createQuery(jpql.toString(), Cinema.class);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            int size = filter.getSize() > 0 ? filter.getSize() : 20;
            int offset = Math.max(0, filter.getPage()) * size;
            query.setFirstResult(offset);
            query.setMaxResults(size);

            return query.getResultList();
        } finally {
            entityManager.close();
        }
    }

    public long countCinemas(CinemaRequest filter) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT COUNT(c) FROM Cinema c WHERE 1=1 ");
            Map<String, Object> params = new HashMap<>();

            if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
                jpql.append("AND c.status = :status ");
                params.put("status", filter.getStatus().trim().toUpperCase());
            }

            if (filter.getCity() != null && !filter.getCity().trim().isEmpty()) {
                jpql.append("AND LOWER(c.city) LIKE :city ");
                params.put("city", "%" + filter.getCity().trim().toLowerCase() + "%");
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

    public List<ShowtimeResponse> findShowtimes(Long cinemaId, LocalDate date, Long movieId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT " +
                    "s.show_time_id, " +
                    "s.movie_id, " +
                    "s.room_id, " +
                    "CAST(s.show_date AS TEXT), " +
                    "CAST(s.start_time AS TEXT), " +
                    "CAST(s.end_time AS TEXT), " +
                    "s.base_price, " +
                    "s.status " +
                    "FROM cinema.show_times s " +
                    "JOIN cinema.rooms r ON s.room_id = r.room_id " +
                    "WHERE r.cinema_id = :cinemaId " +
                    "AND s.show_date = CAST(:showDate AS date) "
            );

            if (movieId != null) {
                sql.append("AND s.movie_id = :movieId ");
            }

            sql.append("ORDER BY s.start_time ASC, s.show_time_id ASC");

            var query = entityManager.createNativeQuery(sql.toString());
            query.setParameter("cinemaId", cinemaId);
            query.setParameter("showDate", date.toString());
            if (movieId != null) {
                query.setParameter("movieId", movieId);
            }

            List<?> rawResults = query.getResultList();
            List<ShowtimeResponse> responses = new ArrayList<>();
            for (Object rowObj : rawResults) {
                Object[] row = (Object[]) rowObj;
                Long showtimeId = row[0] != null ? ((Number) row[0]).longValue() : null;
                Long mId = row[1] != null ? ((Number) row[1]).longValue() : null;
                Long rId = row[2] != null ? ((Number) row[2]).longValue() : null;
                String showDate = row[3] != null ? row[3].toString() : null;
                String startTime = row[4] != null ? row[4].toString() : null;
                String endTime = row[5] != null ? row[5].toString() : null;
                Double basePrice = row[6] != null ? ((Number) row[6]).doubleValue() : null;
                String status = row[7] != null ? row[7].toString() : null;

                responses.add(new ShowtimeResponse(
                        showtimeId, mId, rId, showDate, startTime, endTime, basePrice, status
                ));
            }
            return responses;
        } finally {
            entityManager.close();
        }
    }

    public record CityItem(String cityCode, String cityName) {}

    public List<CityItem> findDistinctCities() {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            String sql = "SELECT DISTINCT c.city FROM cinema.cinemas c WHERE c.status = 'ACTIVE' AND c.city IS NOT NULL AND TRIM(c.city) <> '' ORDER BY c.city ASC";
            var query = entityManager.createNativeQuery(sql);
            List<?> results = query.getResultList();
            List<CityItem> cities = new ArrayList<>();
            for (Object rowObj : results) {
                if (rowObj != null) {
                    String city = rowObj.toString();
                    cities.add(new CityItem(city, city));
                }
            }
            return cities;
        } finally {
            entityManager.close();
        }
    }
}
