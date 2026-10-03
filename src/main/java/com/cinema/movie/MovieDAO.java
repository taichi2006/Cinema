package com.cinema.movie;

import com.cinema.Util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieDAO {

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
            // Bước 1: Truy vấn phân trang lấy danh sách phim (không fetch collection genres)
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
                jpql.append("AND (LOWER(g.genreCode) = :genre OR LOWER(g.genreName) = :genre) ");
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
                    Movie.class
            ).setParameter("movieIds", movieIds).getResultList();

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
                jpql.append("AND (LOWER(g.genreCode) = :genre OR LOWER(g.genreName) = :genre) ");
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
}
