package com.cinema.movie.service;

import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.movie.dao.MovieDAO;
import com.cinema.movie.dao.ReviewDAO;
import com.cinema.movie.entity.Genre;
import com.cinema.movie.entity.Movie;
import com.cinema.movie.entity.Review;
import com.cinema.movie.dto.request.CreateReviewRequest;
import com.cinema.movie.dto.request.MovieRequest;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.dto.response.ReviewResponse;
import com.cinema.movie.dto.response.ShowtimeResponse;

import java.time.LocalDate;
import java.util.List;

public class MovieService {

    private final MovieDAO movieDAO = new MovieDAO();
    private final ReviewDAO reviewDAO = new ReviewDAO();

    // 1. Chi tiết phim
    public MovieResponse getMovieById(long movieId) {
        Movie movie = movieDAO.findById(movieId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy phim"));
        return toDetailResponse(movie);
    }

    // 2. Danh sách phim (tìm kiếm, lọc, phân trang)
    public PageResponse<MovieResponse> getMovies(MovieRequest req) {
        List<MovieResponse> items = movieDAO.findMovies(req).stream()
                .map(this::toSummaryResponse)
                .toList();
        long total = movieDAO.countMovies(req);
        return PageResponse.of(items, new PageMeta(req.getPage(), req.getSize(), total));
    }

    // 3. Suất chiếu của phim
    public PageResponse<ShowtimeResponse> getMovieShowtimes(long movieId, LocalDate date, Long cinemaId, int page, int size) {
        movieDAO.findById(movieId).orElseThrow(() -> ApiException.notFound("Không tìm thấy phim"));
        List<ShowtimeResponse> showtimes = movieDAO.findShowtimes(movieId, date, cinemaId, page, size);
        long total = movieDAO.countShowtimes(movieId, date, cinemaId);
        return PageResponse.of(showtimes, new PageMeta(page, size, total));
    }

    // 4. Đánh giá của phim
    public PageResponse<ReviewResponse> getMovieReviews(long movieId, String sort, int page, int size) {
        movieDAO.findById(movieId).orElseThrow(() -> ApiException.notFound("Không tìm thấy phim"));
        String sortVal = (sort != null && !sort.isBlank()) ? sort.trim() : "createdAt,desc";
        List<ReviewResponse> reviews = reviewDAO.findReviews(movieId, sortVal, page, size);
        long total = reviewDAO.countReviews(movieId);
        return PageResponse.of(reviews, new PageMeta(page, size, total));
    }

    // 5. Gửi đánh giá
    public ReviewResponse createReview(Long userId, long movieId, CreateReviewRequest req) {
        if (userId == null) throw ApiException.unauthorized("Vui lòng đăng nhập để đánh giá phim.");
        if (req == null || req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw ApiException.badRequest("Điểm đánh giá 'rating' phải là số nguyên từ 1 đến 5 sao.");
        }
        movieDAO.findById(movieId).orElseThrow(() -> ApiException.notFound("Không tìm thấy phim"));
        if (!reviewDAO.hasPurchasedTicketForMovie(userId, movieId)) {
            throw ApiException.forbidden("Bạn chỉ có thể đánh giá phim sau khi đã mua vé.");
        }
        if (reviewDAO.hasUserReviewed(userId, movieId)) {
            throw ApiException.conflict("Bạn đã gửi đánh giá cho bộ phim này rồi.");
        }
        return reviewDAO.createReview(userId, movieId, req.getRating(), req.getComment());
    }

    // 6. Thể loại phim
    public List<Genre> getGenres() {
        return movieDAO.findAllGenres();
    }

    private MovieResponse toDetailResponse(Movie m) {
        List<String> genres = m.getGenres() != null ? m.getGenres().stream().map(Genre::getGenreName).toList() : List.of();
        return new MovieResponse(
                String.valueOf(m.getMovieId()), m.getTitle(), m.getDescription(),
                m.getDurationMinutes(), m.getReleaseDate(), m.getPosterUrl(), m.getTrailerUrl(),
                m.getLanguage(), m.getDefaultFormat(), m.getAgeRating(), m.getAgeLimit(),
                m.getStatus(), genres, 0.0, 0
        );
    }

    private MovieResponse toSummaryResponse(Movie m) {
        List<String> genres = m.getGenres() != null ? m.getGenres().stream().map(Genre::getGenreName).toList() : List.of();
        return new MovieResponse(
                String.valueOf(m.getMovieId()), m.getTitle(), m.getPosterUrl(),
                m.getDurationMinutes(), m.getReleaseDate(), genres, m.getStatus(),
                m.getAgeRating(), 0.0, 0
        );
    }
}
