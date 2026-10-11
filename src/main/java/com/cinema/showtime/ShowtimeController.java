package com.cinema.showtime;

import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/showtimes", "/showtimes/*"})
public class ShowtimeController extends BaseServlet {

    private final ShowtimeService service = new ShowtimeService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] segments = getPathSegments(req);

        // GET /showtimes/{id} (Chi tiết suất chiếu)
        if (segments.length == 1) {
            long id = parsePathId(segments[0], "Mã suất chiếu");
            writeSuccess(resp, service.getShowtimeDetail(id));
            return;
        }

        // GET /showtimes/{id}/seats (Sơ đồ ghế)
        if (segments.length == 2 && "seats".equalsIgnoreCase(segments[1])) {
            long id = parsePathId(segments[0], "Mã suất chiếu");
            Long userId = getOptionalUserId(req);
            writeSuccess(resp, service.getSeatMap(id, userId));
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}
