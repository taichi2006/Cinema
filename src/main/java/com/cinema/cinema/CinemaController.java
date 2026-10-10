package com.cinema.cinema;

import com.cinema.cinema.DTO.Request.CinemaRequest;
import com.cinema.common.exception.ApiException;
import com.cinema.common.exception.ErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@WebServlet(urlPatterns = {"/cinema", "/cinema/*"})
public class CinemaController extends HttpServlet {

    private final CinemaService cinemaService = new CinemaService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.trim().isEmpty()) {
                handleGetCinemas(request, response);
                return;
            }

            String[] segments = Arrays.stream(pathInfo.split("/"))
                    .filter(s -> !s.isBlank())
                    .toArray(String[]::new);

            if (segments.length == 1) {
                // GET /cinema/{id}
                handleGetCinemaDetail(request, response, segments[0]);
            } else if (segments.length == 2) {
                String id = segments[0];
                String subResource = segments[1].toLowerCase();
                switch (subResource) {
                    case "showtime", "showtimes" -> handleGetCinemaShowtimes(request, response, id);
                    default -> throw ApiException.notFound("Endpoint không tồn tại: " + subResource);
                }
            } else {
                throw ApiException.notFound("Đường dẫn không hợp lệ.");
            }

        } catch (Exception ex) {
            ErrorHandler.handle(response, ex);
        }
    }

    private void handleGetCinemas(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String city = request.getParameter("city");
        String q = request.getParameter("q");
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

        CinemaRequest cinemaRequest = new CinemaRequest(city, q, page, size, sort);
        Map<String, Object> result = cinemaService.getCinemas(cinemaRequest);

        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), result);
    }

    private void handleGetCinemaDetail(HttpServletRequest request, HttpServletResponse response, String id) throws Exception {
        Map<String, Object> result = cinemaService.getCinemaById(id);
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), result);
    }

    private void handleGetCinemaShowtimes(HttpServletRequest request, HttpServletResponse response, String id) throws Exception {
        String date = request.getParameter("date");
        String movieId = request.getParameter("movieId");
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

        Map<String, Object> result = cinemaService.getCinemaShowtimes(id, date, movieId, page, size);
        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), result);
    }
}
