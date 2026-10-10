package com.cinema.movie;

import com.cinema.common.dto.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.movie.DTO.Request.MovieRequest;
import com.cinema.movie.DTO.Response.MovieResponse;
import com.cinema.movie.DTO.Response.ReviewResponse;
import com.cinema.movie.DTO.Response.ShowtimeResponse;
import jakarta.servlet.annotation.WebServlet;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@WebServlet(urlPatterns = {"/movie", "/movie/*"})
public class MovieController extends BaseServlet {

    private final MovieService movieService = new MovieService();
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String[] segments = getPathSegments(request);

        if (segments.length == 0) {
            handleGetMovies(request, response);
            return;
        }

        if (segments.length == 1) {
            // GET /movie/{id}
            long id = parsePathId(segments[0], "Mã phim");
            handleGetMovieDetail(request, response, id);
        } else if (segments.length == 2) {
            long id = parsePathId(segments[0], "Mã phim");
            String subResource = segments[1].toLowerCase();
            switch (subResource) {
                case "showtime", "showtimes" -> handleGetMovieShowtimes(request, response, id);
                case "review", "reviews" -> handleGetMovieReviews(request, response, id);
                default -> throw ApiException.notFound("Endpoint không tồn tại: " + subResource);
            }
        } else {
            throw ApiException.notFound("Đường dẫn không hợp lệ.");
        }
    }

    private void handleGetMovies(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String q = request.getParameter("q");
        String genre = request.getParameter("genre");
        String status = request.getParameter("status");
        String sort = request.getParameter("sort");
        int page = getIntParam(request, "page", 0);
        int size = getIntParam(request, "size", 20);

        MovieRequest movieRequest = new MovieRequest(q, genre, status, page, size, sort);
        var result = movieService.getMovies(movieRequest);
        writeSuccess(response, result);
    }

    private void handleGetMovieDetail(HttpServletRequest request, HttpServletResponse response, long id) throws IOException {
        var result = movieService.getMovieById(id);
        sendJson(response, result); // Already ApiResponse
    }

    private void handleGetMovieShowtimes(HttpServletRequest request, HttpServletResponse response, long id) throws IOException {
        String dateStr = request.getParameter("date");
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw ApiException.badRequest("Thiếu tham số bắt buộc: date");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Định dạng ngày 'date' không hợp lệ (yêu cầu: YYYY-MM-DD): " + dateStr);
        }

        String cinemaIdStr = request.getParameter("cinemaId");
        Long cinemaId = null;
        if (cinemaIdStr != null && !cinemaIdStr.trim().isEmpty()) {
            cinemaId = parsePathId(cinemaIdStr, "Mã rạp 'cinemaId'");
        }

        int page = getIntParam(request, "page", 0);
        int size = getIntParam(request, "size", 20);

        var result = movieService.getMovieShowtimes(id, date, cinemaId, page, size);
        writeSuccess(response, result);
    }

    private void handleGetMovieReviews(HttpServletRequest request, HttpServletResponse response, long id) throws IOException {
        String sort = request.getParameter("sort");
        int page = getIntParam(request, "page", 0);
        int size = getIntParam(request, "size", 20);

        var result = movieService.getMovieReviews(id, sort, page, size);
        writeSuccess(response, result);
    }
}
