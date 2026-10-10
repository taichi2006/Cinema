package com.cinema.showtime;

import com.cinema.common.exception.ApiException;
import com.cinema.showtime.DTO.Response.SeatMapResponse;
import com.cinema.showtime.DTO.Response.ShowtimeDetailResponse;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;

@WebServlet(urlPatterns = {"/showtime", "/showtime/*"})
public class ShowtimeController extends BaseServlet {

    private final ShowtimeService showtimeService = new ShowtimeService();
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String[] segments = getPathSegments(request);
            if (segments.length == 0) {
                throw ApiException.notFound("Endpoint không tồn tại.");
            }

            if (segments.length == 1) {
                String id = segments[0];
                ShowtimeDetailResponse result = showtimeService.getShowtimeDetail(id);
                writeSuccess(response, result);
            } else if (segments.length == 2) {
                String id = segments[0];
                String subResource = segments[1].toLowerCase();
                switch (subResource) {
                    case "seat", "seats" -> handleGetSeatMap(request, response, id);
                    default -> throw ApiException.notFound("Endpoint không tồn tại: " + subResource);
                }
            } else {
                throw ApiException.notFound("Đường dẫn không hợp lệ.");
            }


    }

    private void handleGetSeatMap(HttpServletRequest request, HttpServletResponse response, String id) throws IOException {
        long userId = getAuthenticatedUserId(request);
        SeatMapResponse result = showtimeService.getSeatMap(id, userId);
        writeSuccess(response, result);
    }
}
