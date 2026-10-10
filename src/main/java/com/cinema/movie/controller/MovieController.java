package com.cinema.movie.controller;

import com.cinema.auth.JwtUtil;
import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.common.exception.ErrorHandler;
import com.cinema.movie.dto.request.MovieRequest;
import com.cinema.movie.dto.request.ReviewRequest;
import com.cinema.movie.dto.response.MovieResponse;
import com.cinema.movie.service.MovieService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@WebServlet(urlPatterns = {"/movies", "/movies/*"})
public class MovieController extends HttpServlet {

    private final MovieService movieService = new MovieService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.trim().isEmpty()) {
                handleGetMovies(request, response);
                return;
            }

            String[] segments = Arrays.stream(pathInfo.split("/"))
                    .filter(s -> !s.isBlank())
                    .toArray(String[]::new);

            if (segments.length == 1) {
                // GET /movies/{movieId}
                handleGetMovieDetail(request, response, segments[0]);
            } else if (segments.length == 2) {
                String id = segments[0];
                String subResource = segments[1].toLowerCase();
                switch (subResource) {
                    case "showtime", "showtimes" -> handleGetMovieShowtimes(request, response, id);
                    case "review", "reviews" -> handleGetMovieReviews(request, response, id);
                    default -> throw ApiException.notFound("Endpoint không tồn tại: " + subResource);
                }
            } else {
                throw ApiException.notFound("Đường dẫn không hợp lệ.");
            }

        } catch (Exception ex) {
            ErrorHandler.handle(response, ex);
        }
    }

    private void handleGetMovies(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String keyword = request.getParameter("keyword");
        if (keyword == null) {
            keyword = request.getParameter("q");
        }

        String genreIdStr = request.getParameter("genreId");
        Integer genreId = null;
        if (genreIdStr != null && !genreIdStr.trim().isEmpty()) {
            try {
                genreId = Integer.parseInt(genreIdStr.trim());
            } catch (NumberFormatException e) {
                throw ApiException.badRequest("Tham số 'genreId' phải là số nguyên.");
            }
        }
        String genre = request.getParameter("genre");

        String status = request.getParameter("status");
        String sort = request.getParameter("sort");
        String pageStr = request.getParameter("page");
        String sizeStr = request.getParameter("size");

        int page = 0;
        if (pageStr != null && !pageStr.trim().isEmpty()) {
            try {
                page = Integer.parseInt(pageStr.trim());
            } catch (NumberFormatException exception) {
                throw ApiException.badRequest("Tham số 'page' phải là một số nguyên hợp lệ.");
            }
        }

        int size = 20;
        if (sizeStr != null && !sizeStr.trim().isEmpty()) {
            try {
                size = Integer.parseInt(sizeStr.trim());
            } catch (NumberFormatException exception) {
                throw ApiException.badRequest("Tham số 'size' phải là một số nguyên hợp lệ.");
            }
        }

        MovieRequest movieRequest = new MovieRequest(keyword, genreId, status, page, size, sort);
        if (genre != null && movieRequest.getGenre() == null) {
            movieRequest.setGenre(genre);
        }

        Map<String, Object> result = movieService.getMovies(movieRequest);

        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), result);
    }

    private void handleGetMovieDetail(HttpServletRequest request, HttpServletResponse response, String id) throws Exception {
        ApiResponse<MovieResponse> result = movieService.getMovieById(id);
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), result);
    }

    private void handleGetMovieShowtimes(HttpServletRequest request, HttpServletResponse response, String id) throws Exception {
        String date = request.getParameter("date");
        String cinemaId = request.getParameter("cinemaId");

        Map<String, Object> result = movieService.getMovieShowtimes(id, date, cinemaId);
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), result);
    }

    private void handleGetMovieReviews(HttpServletRequest request, HttpServletResponse response, String id) throws Exception {
        String sort = request.getParameter("sort");
        String pageStr = request.getParameter("page");
        String sizeStr = request.getParameter("size");

        int page = 0;
        if (pageStr != null && !pageStr.trim().isEmpty()) {
            try {
                page = Integer.parseInt(pageStr.trim());
            } catch (NumberFormatException exception) {
                throw ApiException.badRequest("Tham số 'page' phải là một số nguyên hợp lệ.");
            }
        }

        int size = 20;
        if (sizeStr != null && !sizeStr.trim().isEmpty()) {
            try {
                size = Integer.parseInt(sizeStr.trim());
            } catch (NumberFormatException exception) {
                throw ApiException.badRequest("Tham số 'size' phải là một số nguyên hợp lệ.");
            }
        }

        Map<String, Object> result = movieService.getMovieReviews(id, sort, page, size);
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), result);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.trim().isEmpty()) {
                throw ApiException.notFound("Endpoint không tồn tại.");
            }

            String[] segments = Arrays.stream(pathInfo.split("/"))
                    .filter(s -> !s.isBlank())
                    .toArray(String[]::new);

            if (segments.length == 2) {
                String id = segments[0];
                String subResource = segments[1].toLowerCase();
                switch (subResource) {
                    case "review", "reviews" -> handleCreateOrUpdateReview(request, response, id);
                    default -> throw ApiException.notFound("Endpoint không tồn tại: " + subResource);
                }
            } else {
                throw ApiException.notFound("Đường dẫn không hợp lệ.");
            }

        } catch (Exception ex) {
            ErrorHandler.handle(response, ex);
        }
    }

    private void handleCreateOrUpdateReview(HttpServletRequest request, HttpServletResponse response, String id) throws Exception {
        Long userId = resolveUserId(request);
        ReviewRequest reviewRequest;
        try {
            reviewRequest = objectMapper.readValue(request.getInputStream(), ReviewRequest.class);
        } catch (Exception e) {
            throw ApiException.badRequest("Du lieu khong hop le");
        }

        Map<String, Object> result = movieService.createOrUpdateReview(id, userId, reviewRequest);
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), result);
    }

    private Long resolveUserId(HttpServletRequest request) {
        Object attrUserId = request.getAttribute("userId");
        if (attrUserId instanceof Number num) {
            return num.longValue();
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = JwtUtil.parseAccessToken(token);
                return Long.parseLong(claims.getSubject());
            } catch (JwtException | IllegalArgumentException e) {
                throw new ApiException(401, "Chua dang nhap");
            }
        }

        throw new ApiException(401, "Chua dang nhap");
    }
}
