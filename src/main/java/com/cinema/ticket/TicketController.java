package com.cinema.ticket;

import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/tickets", "/tickets/*"})
public class TicketController extends BaseServlet {

    private final TicketService service = new TicketService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // GET /tickets/{id} (Chi tiết vé)
        if (segments.length == 1) {
            long ticketId = parsePathId(segments[0], "Mã vé");
            writeSuccess(resp, service.getTicketDetail(userId, ticketId));
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}
