package com.cinema.cinema.controller;

import com.cinema.cinema.dto.request.CinemaRequest;
import com.cinema.cinema.service.CinemaService;
import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

@WebServlet(urlPatterns = {"/cinemas", "/cinemas/*"})
public class CinemaController extends BaseServlet {

    private final CinemaService service = new CinemaService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] segments = getPathSegments(req);

        // GET /cinemas (Danh sách rạp)
        if (segments.length == 0) {
            String city = req.getParameter("city");
            String q = req.getParameter("q");
            String sort = req.getParameter("sort");
            int page = getIntParam(req, "page", 0);
            int size = getIntParam(req, "size", 20);

            writeSuccess(resp, service.getCinemas(new CinemaRequest(city, q, page, size, sort)));
            return;
        }

        // GET /cinemas/{id} (Chi tiết rạp)
        if (segments.length == 1) {
            long id = parsePathId(segments[0], "Mã rạp");
            writeSuccess(resp, service.getCinemaById(id));
            return;
        }

        // GET /cinemas/{id}/showtimes (Suất chiếu theo rạp)
        if (segments.length == 2 && "showtimes".equalsIgnoreCase(segments[1])) {
            long id = parsePathId(segments[0], "Mã rạp");
            LocalDate date = parseDateParam(req, "date");
            if (date == null) {
                throw ApiException.badRequest("Tham số 'date' là bắt buộc (định dạng YYYY-MM-DD).");
            }

            String movieIdStr = req.getParameter("movieId");
            Long movieId = (movieIdStr != null && !movieIdStr.isBlank()) ? parsePathId(movieIdStr, "Tham số 'movieId'") : null;
            int page = getIntParam(req, "page", 0);
            int size = getIntParam(req, "size", 20);

            writeSuccess(resp, service.getCinemaShowtimes(id, date, movieId, page, size));
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}
