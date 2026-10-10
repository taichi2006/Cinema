package com.cinema.movie.service;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.common.exception.ApiException;
import com.cinema.movie.dao.MovieDAO;
import com.cinema.movie.dto.request.MovieRequest;
import com.cinema.movie.dto.request.ReviewRequest;
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

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "ACTIVE", "INACTIVE", "COMING_SOON", "ENDED"
    );
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
                .orElseThrow(() -> ApiException.notFound("Khong tim thay phim"));

        Set<String> genreNames = new LinkedHashSet<>();
        if (movie.getGenres() != null) {
            for (Genre genre : movie.getGenres()) {
                if (genre.getGenreName() != null) {
                    genreNames.add(genre.getGenreName());
                }
            }
        }

        MovieResponse response = new MovieResponse(
                movie.getMovieId(),
                movie.getTitle(),
                movie.getDirectorId(),
                movie.getDurationMinutes(),
                movie.getAgeLimit(),
                movie.getDefaultFormat(),
                movie.getDescription(),
                movie.getLanguage(),
                movie.getPosterUrl(),
                movie.getReleaseDate(),
                movie.getStatus()
        );

        return ApiResponse.ok(response);
    }

    public Map<String, Object> getMovieShowtimes(
            String idStr,
            String dateStr,
            String cinemaIdStr
    ) {
        Long movieId = parseAndValidateMovieId(idStr);
        movieDAO.findById(movieId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay phim"));

        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw ApiException.badRequest("Tham so date la bat buoc");
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

        List<ShowtimeResponse> showtimes = movieDAO.findShowtimes(movieId, date, cinemaId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", showtimes);
        return result;
    }

    public Map<String, Object> getMovieShowtimes(
            String idStr,
            String dateStr,
            String cinemaIdStr,
            int page,
            int size
    ) {
        return getMovieShowtimes(idStr, dateStr, cinemaIdStr);
    }

    public Map<String, Object> getMovieReviews(String idStr, String sort, int page, int size) {
        Long movieId = parseAndValidateMovieId(idStr);
        movieDAO.findById(movieId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay phim"));

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
        Map<String, Object> pageData = new LinkedHashMap<>();
        pageData.put("items", reviews);
        pageData.put("meta", meta);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", pageData);
        return result;
    }

    public Map<String, Object> getMovieReviews(String idStr, int page, int size) {
        return getMovieReviews(idStr, "createdAt,desc", page, size);
    }

    public Map<String, Object> getMovieReviews(String idStr) {
        return getMovieReviews(idStr, "createdAt,desc", 0, 20);
    }

    public Map<String, Object> createOrUpdateReview(String idStr, Long userId, ReviewRequest req) {
        if (userId == null) {
            throw new ApiException(401, "Chua dang nhap");
        }

        Long movieId = parseAndValidateMovieId(idStr);
        movieDAO.findById(movieId)
                .orElseThrow(() -> ApiException.notFound("Khong tim thay phim"));

        if (req == null || req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw ApiException.badRequest("So sao phai tu 1-5");
        }

        if (req.getComment() != null && req.getComment().length() > 2000) {
            throw ApiException.badRequest("Binh luan toi da 2000 ky tu");
        }

        MovieDAO.ReviewEligibility eligibility = movieDAO.checkUserReviewEligibility(userId, movieId);
        switch (eligibility) {
            case NOT_WATCHED -> throw new ApiException(422, "Ban chua xem phim nay");
            case NOT_PAID -> throw new ApiException(422, "Chi danh gia don da thanh toan");
            case NOT_ENDED -> throw new ApiException(422, "Chi danh gia sau khi suat chieu ket thuc");
            case ELIGIBLE -> {}
        }

        ReviewResponse response = movieDAO.upsertReview(movieId, userId, req.getRating(), req.getComment());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", response);
        return result;
    }

    private Long parseAndValidateMovieId(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw ApiException.badRequest("Mã phim 'movieId' không được để trống.");
        }
        try {
            long id = Long.parseLong(idStr.trim());
            if (id <= 0) {
                throw ApiException.badRequest("Mã phim 'movieId' phải là số nguyên dương.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("Mã phim 'movieId' không hợp lệ: " + idStr);
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
            MovieResponse response = new MovieResponse(
                    movie.getMovieId(),
                    movie.getTitle(),
                    movie.getDirectorId(),
                    movie.getDurationMinutes(),
                    movie.getAgeLimit(),
                    movie.getDefaultFormat(),
                    movie.getDescription(),
                    movie.getLanguage(),
                    movie.getPosterUrl(),
                    movie.getReleaseDate(),
                    movie.getStatus()
            );
            movieResponses.add(response);
        }

        PageMeta meta = new PageMeta(request.getPage(), request.getSize(), totalElements, totalPages);
        Map<String, Object> pageData = new LinkedHashMap<>();
        pageData.put("items", movieResponses);
        pageData.put("meta", meta);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("data", pageData);
        return result;
    }

    private void validateRequest(MovieRequest request) {
        String keyword = request.getKeyword();
        if (keyword != null && keyword.trim().length() > 100) {
            throw ApiException.badRequest("Từ khóa tìm kiếm không được vượt quá 100 ký tự.");
        }

        if (request.getGenreId() != null && request.getGenreId() <= 0) {
            throw ApiException.badRequest("Mã thể loại 'genreId' phải là số nguyên dương.");
        }

        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            String status = request.getStatus().trim().toUpperCase();
            if ("NOW_SHOWING".equals(status)) {
                request.setStatus("NOW_SHOWING");
            } else if (!ALLOWED_STATUSES.contains(status)) {
                throw ApiException.badRequest("Trang thai phim khong hop le");
            } else {
                request.setStatus(status);
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
