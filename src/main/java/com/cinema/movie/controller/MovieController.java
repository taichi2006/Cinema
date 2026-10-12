package com.cinema.movie.controller;

import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import com.cinema.movie.dto.request.CreateReviewRequest;
import com.cinema.movie.dto.request.MovieRequest;
import com.cinema.movie.service.MovieService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

@WebServlet(urlPatterns = {"/movies", "/movies/*"})
public class MovieController extends BaseServlet {

    private final MovieService service = new MovieService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] segments = getPathSegments(request);

        // GET /movies (Danh sách phim)
        if (segments.length == 0) {
            String q = request.getParameter("q");
            String genre = request.getParameter("genre");
            String status = request.getParameter("status");
            String sort = request.getParameter("sort");
            int page = getIntParam(request, "page", 0);
            int size = getIntParam(request, "size", 20);

            writeSuccess(response, service.getMovies(new MovieRequest(q, genre, status, page, size, sort)));
            return;
        }

        // GET /movies/{id} (Chi tiết phim)
        if (segments.length == 1) {
            long id = parsePathId(segments[0], "Mã phim");
            writeSuccess(response, service.getMovieById(id));
            return;
        }

        // GET /movies/{id}/{subResource}
        if (segments.length == 2) {
            long id = parsePathId(segments[0], "Mã phim");
            String sub = segments[1].toLowerCase();

            if ("showtimes".equals(sub)) {
                LocalDate date = parseDateParam(request, "date");
                if (date == null) {
                    throw ApiException.badRequest("Thiếu tham số bắt buộc: date");
                }
                String cinemaIdStr = request.getParameter("cinemaId");
                Long cinemaId = (cinemaIdStr != null && !cinemaIdStr.isBlank()) ? parsePathId(cinemaIdStr, "Mã rạp 'cinemaId'") : null;
                int page = getIntParam(request, "page", 0);
                int size = getIntParam(request, "size", 20);

                writeSuccess(response, service.getMovieShowtimes(id, date, cinemaId, page, size));
                return;
            }

            if ("reviews".equals(sub)) {
                String sort = request.getParameter("sort");
                int page = getIntParam(request, "page", 0);
                int size = getIntParam(request, "size", 20);

                writeSuccess(response, service.getMovieReviews(id, sort, page, size));
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại: " + sub);
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + request.getRequestURI());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] segments = getPathSegments(request);

        // POST /movies/{id}/reviews (Đánh giá phim)
        if (segments.length == 2 && "reviews".equalsIgnoreCase(segments[1])) {
            long id = parsePathId(segments[0], "Mã phim");
            long userId = getAuthenticatedUserId(request);
            CreateReviewRequest body = parseBody(request, CreateReviewRequest.class);

            response.setStatus(HttpServletResponse.SC_CREATED);
            writeSuccess(response, service.createReview(userId, id, body));
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + request.getRequestURI());
    }
}
