package com.cinema.movie;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.common.exception.ApiException;

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

    public ApiResponse<List<?>> getMovieShowtimes(String idStr) {
        Long movieId = parseAndValidateMovieId(idStr);
        movieDAO.findById(movieId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phim"));
        return ApiResponse.ok(List.of());
    }

    public ApiResponse<List<?>> getMovieReviews(String idStr) {
        Long movieId = parseAndValidateMovieId(idStr);
        movieDAO.findById(movieId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phim"));
        return ApiResponse.ok(List.of());
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
