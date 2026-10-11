package com.cinema.ticket;

import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * GET /tickets/{ticketId}: Chi tiết vé
 * (GET /bookings/{bookingId}/tickets được xử lý tập trung tại BookingController)
 */
@WebServlet(name = "TicketController", urlPatterns = {"/tickets", "/tickets/*"})
public class TicketController extends BaseServlet {

    private final TicketService service;

    public TicketController() {
        this.service = new TicketService();
    }

    public TicketController(TicketService service) {
        this.service = service;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // GET /tickets/{ticketId}
        if (segments.length == 1) {
            long ticketId = parsePathId(segments[0], "Mã vé");
            var ticket = service.getTicketDetail(userId, ticketId);
            writeSuccess(resp, ticket);
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}
