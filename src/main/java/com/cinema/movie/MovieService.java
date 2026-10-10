package com.cinema.movie;

import com.cinema.common.dto.ApiResponse;
import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.movie.DTO.Request.MovieRequest;
import com.cinema.movie.DTO.Response.MovieResponse;
import com.cinema.movie.DTO.Response.ReviewResponse;
import com.cinema.movie.DTO.Response.ShowtimeResponse;

import java.time.LocalDate;
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
    private static final Set<String> ALLOWED_REVIEW_SORTS = Set.of(
            "createdAt,asc", "createdAt,desc",
            "rating,asc", "rating,desc"
    );

    private final MovieDAO movieDAO;

    public MovieService() {
        this.movieDAO = new MovieDAO();
    }

    public MovieService(MovieDAO movieDAO) {
        this.movieDAO = movieDAO;
    }

    public ApiResponse<MovieResponse> getMovieById(long movieId) {
        Movie movie = movieDAO.findById(movieId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phim"));

        Set<String> genreNames = new LinkedHashSet<>();
        if (movie.getGenres() != null) {
            for (Genre genre : movie.getGenres()) {
                if (genre.getGenreName() != null) {
                    genreNames.add(genre.getGenreName());
                }
            }
        }

        MovieResponse response = new MovieResponse(
                String.valueOf(movie.getMovieId()),
                movie.getTitle(),
                movie.getDescription(),
                movie.getDurationMinutes(),
                movie.getReleaseDate(),
                movie.getPosterUrl(),
                movie.getTrailerUrl(),
                movie.getLanguage(),
                movie.getDefaultFormat(),
                movie.getAgeRating(),
                movie.getAgeLimit(),
                movie.getStatus(),
                new ArrayList<>(genreNames),
                0.0,
                0
        );

        return ApiResponse.ok(response);
    }

    public PageResponse<ShowtimeResponse> getMovieShowtimes(
            long movieId,
            LocalDate date,
            Long cinemaId,
            int page,
            int size
    ) {
        movieDAO.findById(movieId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phim"));

        if (page < 0) {
            throw ApiException.badRequest("Số trang 'page' phải lớn hơn hoặc bằng 0.");
        }
        if (size < 1 || size > 50) {
            throw ApiException.badRequest("Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50.");
        }

        List<ShowtimeResponse> showtimes = movieDAO.findShowtimes(movieId, date, cinemaId, page, size);
        long totalElements = movieDAO.countShowtimes(movieId, date, cinemaId);

        PageMeta meta = new PageMeta(page, size, totalElements);
        return PageResponse.of(showtimes, meta);
    }

    public PageResponse<ReviewResponse> getMovieReviews(long movieId, String sort, int page, int size) {
        movieDAO.findById(movieId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phim"));

        if (sort != null && !sort.trim().isEmpty()) {
            if (!ALLOWED_REVIEW_SORTS.contains(sort.trim())) {
                throw ApiException.badRequest(
                        "Tham số 'sort' không hợp lệ: " + sort +
                                ". Các giá trị hợp lệ: [createdAt,asc, createdAt,desc, rating,asc, rating,desc]"
                );
            }
        } else {
            sort = "createdAt,desc";
        }

        if (page < 0) {
            throw ApiException.badRequest("Số trang 'page' phải lớn hơn hoặc bằng 0.");
        }
        if (size < 1 || size > 50) {
            throw ApiException.badRequest("Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50.");
        }

        List<ReviewResponse> reviews = movieDAO.findReviews(movieId, sort.trim(), page, size);
        long totalElements = movieDAO.countReviews(movieId);

        PageMeta meta = new PageMeta(page, size, totalElements);
        return PageResponse.of(reviews, meta);
    }

    public PageResponse<MovieResponse> getMovies(MovieRequest request) {
        validateRequest(request);

        List<Movie> movies = movieDAO.findMovies(request);
        long totalElements = movieDAO.countMovies(request);

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

        PageMeta meta = new PageMeta(request.getPage(), request.getSize(), totalElements);
        return PageResponse.of(movieResponses, meta);
    }

    private void validateRequest(MovieRequest request) {
        if (request.getQ() != null && request.getQ().trim().length() > 100) {
            throw ApiException.badRequest("Từ khóa tìm kiếm 'q' không được vượt quá 100 ký tự.");
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            if (!ALLOWED_STATUSES.contains(request.getStatus().trim())) {
                throw ApiException.badRequest(
                        "Trạng thái 'status' không hợp lệ: " + request.getStatus() +
                                ". Các giá trị hợp lệ: [NOW_SHOWING, COMING_SOON, ENDED]"
                );
            }
        }

        if (request.getSort() != null && !request.getSort().trim().isEmpty()) {
            if (!ALLOWED_SORTS.contains(request.getSort().trim())) {
                throw ApiException.badRequest(
                        "Tham số 'sort' không hợp lệ: " + request.getSort() +
                                ". Các giá trị hợp lệ: [releaseDate,asc, releaseDate,desc, title,asc, title,desc]"
                );
            }
        } else {
            request.setSort("releaseDate,desc");
        }

        if (request.getPage() < 0) {
            throw ApiException.badRequest("Số trang 'page' phải lớn hơn hoặc bằng 0.");
        }

        if (request.getSize() < 1 || request.getSize() > 50) {
            throw ApiException.badRequest("Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50.");
        }

        long offset = (long) request.getPage() * request.getSize();
        if (offset > Integer.MAX_VALUE || offset < 0) {
            throw ApiException.badRequest("Vị trí phân trang vượt quá giới hạn cho phép.");
        }
    }

    public List<Genre> getGenres() {
        return movieDAO.findAllGenres();
    }
}
