package com.cinema.movie;

import com.cinema.common.util.JPAUtil;
import com.cinema.movie.DTO.Request.MovieRequest;
import com.cinema.movie.DTO.Response.ReviewResponse;
import com.cinema.movie.DTO.Response.ShowtimeResponse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MovieDAO {

    public Optional<Movie> findById(Long movieId) {
        if (movieId == null) {
            return Optional.empty();
        }
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            List<Movie> results = entityManager.createQuery(
                    "SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres WHERE m.movieId = :movieId",
                    Movie.class
            ).setParameter("movieId", movieId).getResultList();

            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            entityManager.close();
        }
    }

    public List<ShowtimeResponse> findShowtimes(Long movieId, LocalDate date, Long cinemaId, int page, int size) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT " +
                    "s.show_time_id, " +
                    "s.movie_id, " +
                    "c.cinema_id, " +
                    "c.cinema_name, " +
                    "r.room_id, " +
                    "r.room_name, " +
                    "(s.show_date + s.start_time) AS starts_at, " +
                    "(s.show_date + s.end_time) AS ends_at, " +
                    "m.format, " +
                    "m.language, " +
                    "s.base_price, " +
                    "s.status " +
                    "FROM cinema.show_times s " +
                    "JOIN cinema.rooms r ON s.room_id = r.room_id " +
                    "JOIN cinema.cinemas c ON r.cinema_id = c.cinema_id " +
                    "JOIN cinema.movies m ON s.movie_id = m.movie_id " +
                    "WHERE s.movie_id = :movieId " +
                    "AND s.status = 'SCHEDULED' " +
                    "AND s.show_date = CAST(:showDate AS date) "
            );

            if (cinemaId != null) {
                sql.append("AND c.cinema_id = :cinemaId ");
            }

            sql.append("ORDER BY s.show_date ASC, s.start_time ASC");

            var query = entityManager.createNativeQuery(sql.toString());
            query.setParameter("movieId", movieId);
            query.setParameter("showDate", date.toString());
            if (cinemaId != null) {
                query.setParameter("cinemaId", cinemaId);
            }

            int offset = Math.max(0, page) * Math.max(1, size);
            query.setFirstResult(offset);
            query.setMaxResults(size);

            List<?> rawResults = query.getResultList();
            List<ShowtimeResponse> responses = new ArrayList<>();
            for (Object rowObj : rawResults) {
                Object[] row = (Object[]) rowObj;
                String showtimeId = row[0] != null ? row[0].toString() : null;
                String mId = row[1] != null ? row[1].toString() : null;
                String cId = row[2] != null ? row[2].toString() : null;
                String cName = row[3] != null ? row[3].toString() : null;
                String rId = row[4] != null ? row[4].toString() : null;
                String rName = row[5] != null ? row[5].toString() : null;
                String startsAt = formatTimestamp(row[6]);
                String endsAt = formatTimestamp(row[7]);
                String format = row[8] != null ? row[8].toString() : null;
                String language = row[9] != null ? row[9].toString() : null;
                Long basePrice = row[10] != null ? ((Number) row[10]).longValue() : null;
                String status = row[11] != null ? row[11].toString() : null;

                responses.add(new ShowtimeResponse(
                        showtimeId, mId, cId, cName, rId, rName, startsAt, endsAt, format, language, basePrice, status
                ));
            }
            return responses;
        } finally {
            entityManager.close();
        }
    }

    public long countShowtimes(Long movieId, LocalDate date, Long cinemaId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT COUNT(*) " +
                    "FROM cinema.show_times s " +
                    "JOIN cinema.rooms r ON s.room_id = r.room_id " +
                    "JOIN cinema.cinemas c ON r.cinema_id = c.cinema_id " +
                    "WHERE s.movie_id = :movieId " +
                    "AND s.status = 'SCHEDULED' " +
                    "AND s.show_date = CAST(:showDate AS date) "
            );

            if (cinemaId != null) {
                sql.append("AND c.cinema_id = :cinemaId ");
            }

            var query = entityManager.createNativeQuery(sql.toString());
            query.setParameter("movieId", movieId);
            query.setParameter("showDate", date.toString());
            if (cinemaId != null) {
                query.setParameter("cinemaId", cinemaId);
            }

            Object result = query.getSingleResult();
            return result != null ? ((Number) result).longValue() : 0L;
        } finally {
            entityManager.close();
        }
    }

    public List<ReviewResponse> findReviews(Long movieId, String sort, int page, int size) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT " +
                    "ur.review_id, " +
                    "ur.movie_id, " +
                    "u.full_name, " +
                    "ur.rating, " +
                    "ur.comment, " +
                    "ur.created_at " +
                    "FROM cinema.reviews ur " +
                    "JOIN cinema.users u ON ur.user_id = u.user_id " +
                    "WHERE ur.movie_id = :movieId "
            );

            if ("createdAt,asc".equals(sort)) {
                sql.append("ORDER BY ur.created_at ASC");
            } else if ("rating,desc".equals(sort)) {
                sql.append("ORDER BY ur.rating DESC, ur.created_at DESC");
            } else if ("rating,asc".equals(sort)) {
                sql.append("ORDER BY ur.rating ASC, ur.created_at DESC");
            } else {
                sql.append("ORDER BY ur.created_at DESC");
            }

            var query = entityManager.createNativeQuery(sql.toString());
            query.setParameter("movieId", movieId);

            int offset = Math.max(0, page) * Math.max(1, size);
            query.setFirstResult(offset);
            query.setMaxResults(size);

            List<?> rawResults = query.getResultList();
            List<ReviewResponse> responses = new ArrayList<>();
            for (Object rowObj : rawResults) {
                Object[] row = (Object[]) rowObj;
                String reviewId = row[0] != null ? row[0].toString() : null;
                String mId = row[1] != null ? row[1].toString() : null;
                String authorName = row[2] != null ? row[2].toString() : null;
                Integer rating = row[3] != null ? ((Number) row[3]).intValue() : null;
                String comment = row[4] != null ? row[4].toString() : null;
                String createdAt = formatTimestamp(row[5]);

                responses.add(new ReviewResponse(
                        reviewId, mId, authorName, rating, comment, createdAt
                ));
            }
            return responses;
        } finally {
            entityManager.close();
        }
    }

    public long countReviews(Long movieId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            String sql = "SELECT COUNT(*) " +
                    "FROM cinema.reviews ur " +
                    "WHERE ur.movie_id = :movieId";

            var query = entityManager.createNativeQuery(sql);
            query.setParameter("movieId", movieId);

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

    public List<Movie> findAll() {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            return entityManager
                    .createQuery("SELECT movie FROM Movie movie", Movie.class)
                    .getResultList();
        } finally {
            entityManager.close();
        }
    }

    public List<Movie> findMovies(MovieRequest filter) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            // Bước 1: Truy vấn phân trang lấy danh sách phim (không fetch collection
            // genres)
            StringBuilder jpql = new StringBuilder("SELECT DISTINCT m FROM Movie m ");
            boolean hasGenre = filter.getGenre() != null && !filter.getGenre().trim().isEmpty();
            if (hasGenre) {
                jpql.append("JOIN m.genres g ");
            }

            jpql.append("WHERE 1=1 ");

            boolean hasQ = filter.getQ() != null && !filter.getQ().trim().isEmpty();
            if (hasQ) {
                jpql.append("AND LOWER(m.title) LIKE :q ");
            }

            if (hasGenre) {
                jpql.append("AND LOWER(g.genreName) = :genre ");
            }

            boolean hasStatus = filter.getStatus() != null && !filter.getStatus().trim().isEmpty();
            if (hasStatus) {
                jpql.append("AND m.status = :status ");
            }

            jpql.append(resolveSortClause(filter.getSort()));

            TypedQuery<Movie> query = entityManager.createQuery(jpql.toString(), Movie.class);

            if (hasQ) {
                query.setParameter("q", "%" + filter.getQ().trim().toLowerCase() + "%");
            }
            if (hasGenre) {
                query.setParameter("genre", filter.getGenre().trim().toLowerCase());
            }
            if (hasStatus) {
                query.setParameter("status", filter.getStatus().trim());
            }

            int firstResult = filter.getPage() * filter.getSize();
            query.setFirstResult(firstResult);
            query.setMaxResults(filter.getSize());

            List<Movie> pageMovies = query.getResultList();
            if (pageMovies.isEmpty()) {
                return Collections.emptyList();
            }

            // Bước 2: Tải đầy đủ genres cho các phim trong trang qua truy vấn riêng
            List<Long> movieIds = new ArrayList<>();
            for (Movie movie : pageMovies) {
                movieIds.add(movie.getMovieId());
            }

            List<Movie> moviesWithGenres = entityManager.createQuery(
                    "SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres WHERE m.movieId IN :movieIds",
                    Movie.class).setParameter("movieIds", movieIds).getResultList();

            // Bước 3: Giữ đúng thứ tự sắp xếp ban đầu của danh sách phân trang
            Map<Long, Movie> movieMap = new HashMap<>();
            for (Movie movie : moviesWithGenres) {
                movieMap.put(movie.getMovieId(), movie);
            }

            List<Movie> orderedMovies = new ArrayList<>();
            for (Movie movie : pageMovies) {
                Movie loaded = movieMap.get(movie.getMovieId());
                orderedMovies.add(loaded != null ? loaded : movie);
            }

            return orderedMovies;

        } finally {
            entityManager.close();
        }
    }

    public long countMovies(MovieRequest filter) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT COUNT(DISTINCT m) FROM Movie m ");
            boolean hasGenre = filter.getGenre() != null && !filter.getGenre().trim().isEmpty();
            if (hasGenre) {
                jpql.append("JOIN m.genres g ");
            }

            jpql.append("WHERE 1=1 ");

            boolean hasQ = filter.getQ() != null && !filter.getQ().trim().isEmpty();
            if (hasQ) {
                jpql.append("AND LOWER(m.title) LIKE :q ");
            }

            if (hasGenre) {
                jpql.append("AND LOWER(g.genreName) = :genre ");
            }

            boolean hasStatus = filter.getStatus() != null && !filter.getStatus().trim().isEmpty();
            if (hasStatus) {
                jpql.append("AND m.status = :status ");
            }

            TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class);

            if (hasQ) {
                query.setParameter("q", "%" + filter.getQ().trim().toLowerCase() + "%");
            }
            if (hasGenre) {
                query.setParameter("genre", filter.getGenre().trim().toLowerCase());
            }
            if (hasStatus) {
                query.setParameter("status", filter.getStatus().trim());
            }

            return query.getSingleResult();

        } finally {
            entityManager.close();
        }
    }

    private String resolveSortClause(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "ORDER BY m.releaseDate DESC, m.movieId DESC ";
        }
        return switch (sort.trim()) {
            case "releaseDate,asc" -> "ORDER BY m.releaseDate ASC, m.movieId ASC ";
            case "releaseDate,desc" -> "ORDER BY m.releaseDate DESC, m.movieId DESC ";
            case "title,asc" -> "ORDER BY m.title ASC, m.movieId ASC ";
            case "title,desc" -> "ORDER BY m.title DESC, m.movieId DESC ";
            default -> "ORDER BY m.releaseDate DESC, m.movieId DESC ";
        };
    }

    public List<Genre> findAllGenres() {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            return entityManager.createQuery("SELECT g FROM Genre g ORDER BY g.genreName ASC", Genre.class)
                    .getResultList();
        } finally {
            entityManager.close();
        }
    }
}
