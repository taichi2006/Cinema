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

    public SuccessEnvelope<List<MovieResponse>> getMovies(MovieRequest request) {
        validateRequest(request);

        List<Movie> movies = movieDAO.findMovies(request);
        long totalElements = movieDAO.countMovies(request);
        int totalPages = request.getSize() > 0
                ? (int) Math.ceil((double) totalElements / request.getSize())
                : 0;

        List<MovieResponse> movieResponses = new ArrayList<>();
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

            MovieResponse response = new MovieResponse(
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
            movieResponses.add(response);
        }

        PageMeta meta = new PageMeta(request.getPage(), request.getSize(), totalElements, totalPages);
        return new SuccessEnvelope<>(movieResponses, meta);
    }

    private void validateRequest(MovieRequest request) {
        if (request.getQ() != null && request.getQ().trim().length() > 100) {
            throw new InvalidFilterException("q", "Từ khóa tìm kiếm 'q' không được vượt quá 100 ký tự.");
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            if (!ALLOWED_STATUSES.contains(request.getStatus().trim())) {
                throw new InvalidFilterException(
                        "status",
                        "Trạng thái 'status' không hợp lệ: " + request.getStatus() +
                                ". Các giá trị hợp lệ: [NOW_SHOWING, COMING_SOON, ENDED]"
                );
            }
        }

        if (request.getSort() != null && !request.getSort().trim().isEmpty()) {
            if (!ALLOWED_SORTS.contains(request.getSort().trim())) {
                throw new InvalidFilterException(
                        "sort",
                        "Tham số 'sort' không hợp lệ: " + request.getSort() +
                                ". Các giá trị hợp lệ: [releaseDate,asc, releaseDate,desc, title,asc, title,desc]"
                );
            }
        } else {
            request.setSort("releaseDate,desc");
        }

        if (request.getPage() < 0) {
            throw new InvalidFilterException("page", "Số trang 'page' phải lớn hơn hoặc bằng 0.");
        }

        if (request.getSize() < 1 || request.getSize() > 50) {
            throw new InvalidFilterException("size", "Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50.");
        }

        long offset = (long) request.getPage() * request.getSize();
        if (offset > Integer.MAX_VALUE || offset < 0) {
            throw new InvalidFilterException("page", "Vị trí phân trang vượt quá giới hạn cho phép.");
        }
    }
}
