package com.cinema.movie;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MovieService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("NOW_SHOWING", "COMING_SOON", "ENDED");
    private static final Set<String> ALLOWED_SORTS = Set.of(
            "releaseDate,asc", "releaseDate,desc",
            "title,asc", "title,desc"
    );

    private final MovieDAO movieDAO;

    public MovieService() {
        this.movieDAO = new MovieDAO();
    }

    public MovieService(MovieDAO movieDAO) {
        this.movieDAO = movieDAO;
    }

    public SuccessEnvelope<List<MovieSummary>> getMovies(MovieFilter filter) {
        return getMovies(filter, java.util.UUID.randomUUID().toString());
    }

    public SuccessEnvelope<List<MovieSummary>> getMovies(MovieFilter filter, String traceId) {
        validateFilter(filter);

        List<Movie> movies = movieDAO.findMovies(filter);
        long totalElements = movieDAO.countMovies(filter);
        int totalPages = filter.getSize() > 0
                ? (int) Math.ceil((double) totalElements / filter.getSize())
                : 0;

        List<MovieSummary> summaries = new ArrayList<>();
        for (Movie movie : movies) {
            Set<String> genreNames = new LinkedHashSet<>();
            if (movie.getGenres() != null) {
                for (Genre genre : movie.getGenres()) {
                    if (genre.getGenreName() != null) {
                        genreNames.add(genre.getGenreName());
                    }
                }
            }

            String id = String.valueOf(movie.getMovieId());
            Double averageRating = 0.0;
            Integer reviewCount = 0;

            MovieSummary summary = new MovieSummary(
                    id,
                    movie.getTitle(),
                    movie.getPosterUrl(),
                    movie.getDurationMinutes(),
                    movie.getReleaseDate(),
                    new ArrayList<>(genreNames),
                    movie.getStatus(),
                    movie.getAgeRating(),
                    averageRating,
                    reviewCount
            );
            summaries.add(summary);
        }

        PageMeta meta = new PageMeta(filter.getPage(), filter.getSize(), totalElements, totalPages);
        String finalTraceId = (traceId != null && !traceId.trim().isEmpty())
                ? traceId
                : java.util.UUID.randomUUID().toString();
        return new SuccessEnvelope<>(summaries, meta, finalTraceId);
    }

    public List<MovieResponse> getAllMovies() {
        List<Movie> movies = movieDAO.findAll();
        List<MovieResponse> movieResponses = new ArrayList<>();
        for (Movie movie : movies) {
            MovieResponse movieResponse = new MovieResponse(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDurationMinutes()
            );
            movieResponses.add(movieResponse);
        }
        return movieResponses;
    }

    private void validateFilter(MovieFilter filter) {
        if (filter.getQ() != null && filter.getQ().trim().length() > 100) {
            throw new InvalidFilterException("q", "Từ khóa tìm kiếm 'q' không được vượt quá 100 ký tự.");
        }

        if (filter.getStatus() != null && !filter.getStatus().trim().isEmpty()) {
            if (!ALLOWED_STATUSES.contains(filter.getStatus().trim())) {
                throw new InvalidFilterException(
                        "status",
                        "Trạng thái 'status' không hợp lệ: " + filter.getStatus() +
                                ". Các giá trị hợp lệ: [NOW_SHOWING, COMING_SOON, ENDED]"
                );
            }
        }

        if (filter.getSort() != null && !filter.getSort().trim().isEmpty()) {
            if (!ALLOWED_SORTS.contains(filter.getSort().trim())) {
                throw new InvalidFilterException(
                        "sort",
                        "Tham số 'sort' không hợp lệ: " + filter.getSort() +
                                ". Các giá trị hợp lệ: [releaseDate,asc, releaseDate,desc, title,asc, title,desc]"
                );
            }
        } else {
            filter.setSort("releaseDate,desc");
        }

        if (filter.getPage() < 0) {
            throw new InvalidFilterException("page", "Số trang 'page' phải lớn hơn hoặc bằng 0.");
        }

        if (filter.getSize() < 1 || filter.getSize() > 50) {
            throw new InvalidFilterException("size", "Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50.");
        }

        long offset = (long) filter.getPage() * filter.getSize();
        if (offset > Integer.MAX_VALUE || offset < 0) {
            throw new InvalidFilterException("page", "Vị trí phân trang vượt quá giới hạn cho phép.");
        }
    }
}
