package com.cinema.movie.dao;

import com.cinema.common.util.JPAUtil;
import com.cinema.movie.dto.request.MovieRequest;
import com.cinema.movie.dto.response.ReviewResponse;
import com.cinema.movie.dto.response.ShowtimeResponse;
import com.cinema.movie.entity.Genre;
import com.cinema.movie.entity.Movie;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
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

    public List<ShowtimeResponse> findShowtimes(Long movieId, LocalDate date, Long cinemaId) {
        return findShowtimes(movieId, date, cinemaId, 0, Integer.MAX_VALUE);
    }

    public List<ShowtimeResponse> findShowtimes(Long movieId, LocalDate date, Long cinemaId, int page, int size) {
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
                    "WHERE s.movie_id = :movieId " +
                    "AND s.show_date = CAST(:showDate AS date) "
            );

            if (cinemaId != null) {
                sql.append("AND r.cinema_id = :cinemaId ");
            }

            sql.append("ORDER BY s.start_time ASC");

            var query = entityManager.createNativeQuery(sql.toString());
            query.setParameter("movieId", movieId);
            query.setParameter("showDate", date.toString());
            if (cinemaId != null) {
                query.setParameter("cinemaId", cinemaId);
            }

            if (size > 0 && size < Integer.MAX_VALUE) {
                int offset = Math.max(0, page) * size;
                query.setFirstResult(offset);
                query.setMaxResults(size);
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

    public enum ReviewEligibility {
        ELIGIBLE,
        NOT_WATCHED,
        NOT_PAID,
        NOT_ENDED
    }

    public List<ReviewResponse> findReviews(Long movieId, String sort, int page, int size) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder sql = new StringBuilder(
                    "SELECT " +
                    "ur.review_id, " +
                    "ur.movie_id, " +
                    "ur.user_id, " +
                    "ur.rating, " +
                    "ur.comment, " +
                    "ur.created_at " +
                    "FROM cinema.reviews ur " +
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
                Long reviewId = row[0] != null ? ((Number) row[0]).longValue() : null;
                Long mId = row[1] != null ? ((Number) row[1]).longValue() : null;
                Long userId = row[2] != null ? ((Number) row[2]).longValue() : null;
                Integer rating = row[3] != null ? ((Number) row[3]).intValue() : null;
                String comment = row[4] != null ? row[4].toString() : null;
                String createdAt = formatTimestamp(row[5]);

                responses.add(new ReviewResponse(
                        reviewId, mId, userId, rating, comment, createdAt
                ));
            }
            return responses;
        } finally {
            entityManager.close();
        }
    }

    public List<ReviewResponse> findReviews(Long movieId, int page, int size) {
        return findReviews(movieId, "createdAt,desc", page, size);
    }

    public ReviewEligibility checkUserReviewEligibility(Long userId, Long movieId) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            String sql = "SELECT " +
                    "b.status AS booking_status, " +
                    "COALESCE(t.status, 'NO_TICKET') AS ticket_status " +
                    "FROM cinema.bookings b " +
                    "JOIN cinema.show_time_seats sts ON b.booking_id = sts.booking_id " +
                    "JOIN cinema.show_times st ON sts.show_time_id = st.show_time_id " +
                    "LEFT JOIN cinema.tickets t ON sts.id = t.show_time_seat_id " +
                    "WHERE b.user_id = :userId AND st.movie_id = :movieId";

            var query = entityManager.createNativeQuery(sql);
            query.setParameter("userId", userId);
            query.setParameter("movieId", movieId);

            List<?> rows = query.getResultList();
            if (rows.isEmpty()) {
                return ReviewEligibility.NOT_WATCHED;
            }

            boolean hasUsedTicket = false;
            boolean hasConfirmedBooking = false;

            for (Object rowObj : rows) {
                Object[] row = (Object[]) rowObj;
                String bookingStatus = row[0] != null ? row[0].toString() : "";
                String ticketStatus = row[1] != null ? row[1].toString() : "";

                if ("USED".equalsIgnoreCase(ticketStatus)) {
                    hasUsedTicket = true;
                }
                if ("CONFIRMED".equalsIgnoreCase(bookingStatus)) {
                    hasConfirmedBooking = true;
                }
            }

            if (hasUsedTicket) {
                return ReviewEligibility.ELIGIBLE;
            }
            if (!hasConfirmedBooking) {
                return ReviewEligibility.NOT_PAID;
            }
            return ReviewEligibility.NOT_ENDED;
        } finally {
            entityManager.close();
        }
    }

    public ReviewResponse upsertReview(Long movieId, Long userId, Integer rating, String comment) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            String sql = "INSERT INTO cinema.reviews (movie_id, user_id, rating, comment, created_at) " +
                    "VALUES (:movieId, :userId, :rating, :comment, CURRENT_TIMESTAMP) " +
                    "ON CONFLICT (user_id, movie_id) " +
                    "DO UPDATE SET rating = EXCLUDED.rating, comment = EXCLUDED.comment, created_at = CURRENT_TIMESTAMP " +
                    "RETURNING review_id, movie_id, user_id, rating, comment, created_at";

            var query = entityManager.createNativeQuery(sql);
            query.setParameter("movieId", movieId);
            query.setParameter("userId", userId);
            query.setParameter("rating", rating);
            query.setParameter("comment", comment);

            Object[] row = (Object[]) query.getSingleResult();
            transaction.commit();

            Long reviewId = row[0] != null ? ((Number) row[0]).longValue() : null;
            Long mId = row[1] != null ? ((Number) row[1]).longValue() : null;
            Long uId = row[2] != null ? ((Number) row[2]).longValue() : null;
            Integer r = row[3] != null ? ((Number) row[3]).intValue() : null;
            String c = row[4] != null ? row[4].toString() : null;
            String createdAt = formatTimestamp(row[5]);

            return new ReviewResponse(reviewId, mId, uId, r, c, createdAt);
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
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
            boolean hasKeyword = (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty())
                    || (filter.getQ() != null && !filter.getQ().trim().isEmpty());
            String keywordVal = filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()
                    ? filter.getKeyword().trim().toLowerCase()
                    : (filter.getQ() != null ? filter.getQ().trim().toLowerCase() : "");

            boolean hasGenreId = filter.getGenreId() != null;
            boolean hasGenre = !hasGenreId && filter.getGenre() != null && !filter.getGenre().trim().isEmpty();

            if (hasGenreId || hasGenre) {
                jpql.append("JOIN m.genres g ");
            }

            jpql.append("WHERE 1=1 ");

            if (hasKeyword) {
                jpql.append("AND LOWER(m.title) LIKE :keyword ");
            }

            if (hasGenreId) {
                jpql.append("AND g.genreId = :genreId ");
            } else if (hasGenre) {
                jpql.append("AND LOWER(g.genreName) = :genre ");
            }

            boolean hasStatus = filter.getStatus() != null && !filter.getStatus().trim().isEmpty();
            if (hasStatus) {
                jpql.append("AND m.status = :status ");
            }

            jpql.append(resolveSortClause(filter.getSort()));

            TypedQuery<Movie> query = entityManager.createQuery(jpql.toString(), Movie.class);

            if (hasKeyword) {
                query.setParameter("keyword", "%" + keywordVal + "%");
            }
            if (hasGenreId) {
                query.setParameter("genreId", filter.getGenreId().longValue());
            } else if (hasGenre) {
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
            boolean hasKeyword = (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty())
                    || (filter.getQ() != null && !filter.getQ().trim().isEmpty());
            String keywordVal = filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()
                    ? filter.getKeyword().trim().toLowerCase()
                    : (filter.getQ() != null ? filter.getQ().trim().toLowerCase() : "");

            boolean hasGenreId = filter.getGenreId() != null;
            boolean hasGenre = !hasGenreId && filter.getGenre() != null && !filter.getGenre().trim().isEmpty();

            if (hasGenreId || hasGenre) {
                jpql.append("JOIN m.genres g ");
            }

            jpql.append("WHERE 1=1 ");

            if (hasKeyword) {
                jpql.append("AND LOWER(m.title) LIKE :keyword ");
            }

            if (hasGenreId) {
                jpql.append("AND g.genreId = :genreId ");
            } else if (hasGenre) {
                jpql.append("AND LOWER(g.genreName) = :genre ");
            }

            boolean hasStatus = filter.getStatus() != null && !filter.getStatus().trim().isEmpty();
            if (hasStatus) {
                jpql.append("AND m.status = :status ");
            }

            TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class);

            if (hasKeyword) {
                query.setParameter("keyword", "%" + keywordVal + "%");
            }
            if (hasGenreId) {
                query.setParameter("genreId", filter.getGenreId().longValue());
            } else if (hasGenre) {
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
