package com.cinema.movie.service;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.common.exception.ApiException;
import com.cinema.movie.dao.MovieDAO;
import com.cinema.movie.dto.request.MovieRequest;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.dto.response.ReviewResponse;
import com.cinema.movie.dto.response.ShowtimeResponse;
import com.cinema.movie.entity.Genre;
import com.cinema.movie.entity.Movie;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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

    public ApiResponse<MovieResponse> getMovieById(String idStr) {
        Long movieId = parseAndValidateMovieId(idStr);
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

    public Map<String, Object> getMovieShowtimes(
            String idStr,
            String dateStr,
            String cinemaIdStr,
            int page,
            int size
    ) {
        Long movieId = parseAndValidateMovieId(idStr);
        movieDAO.findById(movieId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phim"));

        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw ApiException.badRequest("Thiếu tham số bắt buộc: date");
        }

        LocalDate date;
        try {
            date = LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Định dạng ngày 'date' không hợp lệ (yêu cầu: YYYY-MM-DD): " + dateStr);
        }

        Long cinemaId = null;
        if (cinemaIdStr != null && !cinemaIdStr.trim().isEmpty()) {
            try {
                cinemaId = Long.parseLong(cinemaIdStr.trim());
                if (cinemaId <= 0) {
                    throw ApiException.badRequest("Mã rạp 'cinemaId' phải là số nguyên dương.");
                }
            } catch (NumberFormatException e) {
                throw ApiException.badRequest("Mã rạp 'cinemaId' không hợp lệ: " + cinemaIdStr);
            }
        }

        if (page < 0) {
            throw ApiException.badRequest("Số trang 'page' phải lớn hơn hoặc bằng 0.");
        }
        if (size < 1 || size > 50) {
            throw ApiException.badRequest("Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50.");
        }

        List<ShowtimeResponse> showtimes = movieDAO.findShowtimes(movieId, date, cinemaId, page, size);
        long totalElements = movieDAO.countShowtimes(movieId, date, cinemaId);
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        PageMeta meta = new PageMeta(page, size, totalElements, totalPages);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", showtimes);
        result.put("meta", meta);
        return result;
    }

    public Map<String, Object> getMovieReviews(String idStr, String sort, int page, int size) {
        Long movieId = parseAndValidateMovieId(idStr);
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
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        PageMeta meta = new PageMeta(page, size, totalElements, totalPages);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", reviews);
        result.put("meta", meta);
        return result;
    }

    public Map<String, Object> getMovieReviews(String idStr) {
        return getMovieReviews(idStr, "createdAt,desc", 0, 20);
    }

    private Long parseAndValidateMovieId(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw ApiException.badRequest("Mã phim 'id' không được để trống.");
        }
        try {
            long id = Long.parseLong(idStr.trim());
            if (id <= 0) {
                throw ApiException.badRequest("Mã phim 'id' phải là số nguyên dương.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Mã phim 'id' không hợp lệ: " + idStr);
        }
    }

    public Map<String, Object> getMovies(MovieRequest request) {
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
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", movieResponses);
        result.put("meta", meta);
        return result;
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
