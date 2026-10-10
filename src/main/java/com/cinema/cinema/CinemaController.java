package com.cinema.cinema;

import com.cinema.cinema.DTO.Request.CinemaRequest;
import com.cinema.cinema.DTO.Response.CinemaResponse;
import com.cinema.cinema.DTO.Response.CinemaShowtimeResponse;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.exception.ApiException;
import jakarta.servlet.annotation.WebServlet;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@WebServlet(urlPatterns = {"/cinema", "/cinema/*"})
public class CinemaController extends BaseServlet {

    private final CinemaService cinemaService = new CinemaService();
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String[] segments = getPathSegments(request);

        if (segments.length == 0) {
            handleGetCinemas(request, response);
            return;
        }

        if (segments.length == 1) {
            // GET /cinema/{id}
            long id = parsePathId(segments[0], "Mã rạp");
            handleGetCinemaDetail(request, response, id);
        } else if (segments.length == 2) {
            long id = parsePathId(segments[0], "Mã rạp");
            String subResource = segments[1].toLowerCase();
            switch (subResource) {
                case "showtime", "showtimes" -> handleGetCinemaShowtimes(request, response, id);
                default -> throw ApiException.notFound("Endpoint không tồn tại: " + subResource);
            }
        } else {
            throw ApiException.notFound("Đường dẫn không hợp lệ.");
        }
    }

    private void handleGetCinemas(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String city = request.getParameter("city");
        String q = request.getParameter("q");
        String sort = request.getParameter("sort");
        int page = getIntParam(request, "page", 0);
        int size = getIntParam(request, "size", 20);

        CinemaRequest cinemaRequest = new CinemaRequest(city, q, page, size, sort);
        PageResponse<CinemaResponse> result = cinemaService.getCinemas(cinemaRequest);
        writeSuccess(response, result);
    }

    private void handleGetCinemaDetail(HttpServletRequest request, HttpServletResponse response, long id) throws IOException {
        CinemaResponse result = cinemaService.getCinemaById(id);
        writeSuccess(response, result);
    }

    private void handleGetCinemaShowtimes(HttpServletRequest request, HttpServletResponse response, long id) throws IOException {
        String dateStr = request.getParameter("date");
        if (dateStr == null || dateStr.trim().isEmpty()) {
            throw ApiException.badRequest("Tham số 'date' là bắt buộc (định dạng YYYY-MM-DD).");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Tham số 'date' không hợp lệ hoặc sai định dạng YYYY-MM-DD: " + dateStr);
        }

        String movieIdStr = request.getParameter("movieId");
        Long movieId = null;
        if (movieIdStr != null && !movieIdStr.trim().isEmpty()) {
            movieId = parsePathId(movieIdStr, "Tham số 'movieId'");
        }

        int page = getIntParam(request, "page", 0);
        int size = getIntParam(request, "size", 20);

        PageResponse<CinemaShowtimeResponse> result = cinemaService.getCinemaShowtimes(id, date, movieId, page, size);
        writeSuccess(response, result);
    }
}
