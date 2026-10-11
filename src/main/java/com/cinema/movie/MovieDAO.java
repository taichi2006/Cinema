package com.cinema.movie;

import com.cinema.cinema.Cinema;
import com.cinema.cinema.Room;
import com.cinema.common.util.JPAUtil;
import com.cinema.movie.dto.request.MovieRequest;
import com.cinema.movie.dto.response.ShowtimeResponse;
import com.cinema.showtime.Showtime;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MovieDAO {

    public MovieDAO() {}

    public Optional<Movie> findById(Long movieId) {
        if (movieId == null) return Optional.empty();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            List<Movie> results = em.createQuery(
                    "SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres WHERE m.movieId = :movieId",
                    Movie.class
            ).setParameter("movieId", movieId).getResultList();
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }

    public List<Movie> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT m FROM Movie m", Movie.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<Movie> findMovies(MovieRequest filter) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT DISTINCT m FROM Movie m ");
            boolean hasGenre = filter.getGenre() != null && !filter.getGenre().trim().isEmpty();
            if (hasGenre) jpql.append("JOIN m.genres g ");
            jpql.append("WHERE 1=1 ");

            boolean hasQ = filter.getQ() != null && !filter.getQ().trim().isEmpty();
            if (hasQ) jpql.append("AND LOWER(m.title) LIKE :q ");
            if (hasGenre) jpql.append("AND LOWER(g.genreName) = :genre ");
            boolean hasStatus = filter.getStatus() != null && !filter.getStatus().trim().isEmpty();
            if (hasStatus) jpql.append("AND m.status = :status ");

            jpql.append(resolveSortClause(filter.getSort()));

            TypedQuery<Movie> query = em.createQuery(jpql.toString(), Movie.class);
            if (hasQ) query.setParameter("q", "%" + filter.getQ().trim().toLowerCase() + "%");
            if (hasGenre) query.setParameter("genre", filter.getGenre().trim().toLowerCase());
            if (hasStatus) query.setParameter("status", filter.getStatus().trim());

            query.setFirstResult(filter.getPage() * filter.getSize());
            query.setMaxResults(filter.getSize());

            List<Movie> pageMovies = query.getResultList();
            for (Movie m : pageMovies) {
                m.getGenres().size(); // Nạp sẵn genres (BatchSize gom batch tự động không bị N+1)
            }
            return pageMovies;
        } finally {
            em.close();
        }
    }

    public long countMovies(MovieRequest filter) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT COUNT(DISTINCT m) FROM Movie m ");
            boolean hasGenre = filter.getGenre() != null && !filter.getGenre().trim().isEmpty();
            if (hasGenre) jpql.append("JOIN m.genres g ");
            jpql.append("WHERE 1=1 ");

            boolean hasQ = filter.getQ() != null && !filter.getQ().trim().isEmpty();
            if (hasQ) jpql.append("AND LOWER(m.title) LIKE :q ");
            if (hasGenre) jpql.append("AND LOWER(g.genreName) = :genre ");
            boolean hasStatus = filter.getStatus() != null && !filter.getStatus().trim().isEmpty();
            if (hasStatus) jpql.append("AND m.status = :status ");

            TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
            if (hasQ) query.setParameter("q", "%" + filter.getQ().trim().toLowerCase() + "%");
            if (hasGenre) query.setParameter("genre", filter.getGenre().trim().toLowerCase());
            if (hasStatus) query.setParameter("status", filter.getStatus().trim());

            return query.getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<ShowtimeResponse> findShowtimes(Long movieId, LocalDate date, Long cinemaId, int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT s, m, r, c FROM Showtime s, Room r, Cinema c, Movie m " +
                    "WHERE s.roomId = r.roomId AND r.cinemaId = c.cinemaId AND s.movieId = m.movieId " +
                    "AND s.movieId = :movieId AND s.status = 'SCHEDULED' AND s.showDate = :showDate "
            );
            if (cinemaId != null) jpql.append("AND c.cinemaId = :cinemaId ");
            jpql.append("ORDER BY s.showDate ASC, s.startTime ASC");

            TypedQuery<Object[]> query = em.createQuery(jpql.toString(), Object[].class)
                    .setParameter("movieId", movieId.intValue())
                    .setParameter("showDate", date)
                    .setFirstResult(Math.max(0, page) * Math.max(1, size))
                    .setMaxResults(size);
            if (cinemaId != null) query.setParameter("cinemaId", cinemaId);

            List<ShowtimeResponse> responses = new ArrayList<>();
            for (Object[] row : query.getResultList()) {
                Showtime s = (Showtime) row[0];
                Movie m = (Movie) row[1];
                Room r = (Room) row[2];
                Cinema c = (Cinema) row[3];
                responses.add(new ShowtimeResponse(
                        String.valueOf(s.getShowTimeId()), String.valueOf(s.getMovieId()),
                        String.valueOf(c.getCinemaId()), c.getCinemaName(),
                        String.valueOf(r.getRoomId()), r.getRoomName(),
                        formatDateTime(s.getShowDate(), s.getStartTime()),
                        formatDateTime(s.getShowDate(), s.getEndTime()),
                        m.getFormat(), m.getLanguage(),
                        s.getBasePrice() != null ? s.getBasePrice().longValue() : 0L, s.getStatus()
                ));
            }
            return responses;
        } finally {
            em.close();
        }
    }

    public long countShowtimes(Long movieId, LocalDate date, Long cinemaId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder(
                    "SELECT COUNT(s) FROM Showtime s, Room r, Cinema c " +
                    "WHERE s.roomId = r.roomId AND r.cinemaId = c.cinemaId " +
                    "AND s.movieId = :movieId AND s.status = 'SCHEDULED' AND s.showDate = :showDate "
            );
            if (cinemaId != null) jpql.append("AND c.cinemaId = :cinemaId ");

            TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class)
                    .setParameter("movieId", movieId.intValue())
                    .setParameter("showDate", date);
            if (cinemaId != null) query.setParameter("cinemaId", cinemaId);

            return query.getSingleResult();
        } finally {
            em.close();
        }
    }

    public List<Genre> findAllGenres() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT g FROM Genre g ORDER BY g.genreName ASC", Genre.class).getResultList();
        } finally {
            em.close();
        }
    }

    private String resolveSortClause(String sort) {
        if (sort == null || sort.trim().isEmpty()) return "ORDER BY m.releaseDate DESC, m.movieId DESC ";
        return switch (sort.trim()) {
            case "releaseDate,asc" -> "ORDER BY m.releaseDate ASC, m.movieId ASC ";
            case "releaseDate,desc" -> "ORDER BY m.releaseDate DESC, m.movieId DESC ";
            case "title,asc" -> "ORDER BY m.title ASC, m.movieId ASC ";
            case "title,desc" -> "ORDER BY m.title DESC, m.movieId DESC ";
            default -> "ORDER BY m.releaseDate DESC, m.movieId DESC ";
        };
    }

    private String formatDateTime(LocalDate date, LocalTime time) {
        if (date == null || time == null) return null;
        return LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toString();
    }
}
