package com.cinema.ticket;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.common.exception.ErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 1. GET /bookings/{bookingId}/tickets: Danh sách vé của booking
 * 2. GET /tickets/{ticketId}: Chi tiết vé
 */
@WebServlet(name = "TicketController", urlPatterns = {"/tickets", "/tickets/*", "/bookings/*"})
public class TicketController extends HttpServlet {

    private static final Pattern BOOKING_TICKETS_PATTERN = Pattern.compile("^/bookings/(\\d+)/tickets/?$");
    private static final Pattern TICKET_DETAIL_PATTERN = Pattern.compile("^/tickets/(\\d+)/?$");

    private final TicketService service = new TicketService();
    private final ObjectMapper json = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long userId = getUserId(req);
            String path = getNormalizedPath(req);

            // 1. GET /bookings/{bookingId}/tickets
            Matcher bookingMatcher = BOOKING_TICKETS_PATTERN.matcher(path);
            if (bookingMatcher.matches()) {
                long bookingId = Long.parseLong(bookingMatcher.group(1));
                var tickets = service.getTicketsByBooking(userId, bookingId);
                writeJson(resp, HttpServletResponse.SC_OK, ApiResponse.ok(tickets));
                return;
            }

            // 2. GET /tickets/{ticketId}
            Matcher ticketMatcher = TICKET_DETAIL_PATTERN.matcher(path);
            if (ticketMatcher.matches()) {
                long ticketId = Long.parseLong(ticketMatcher.group(1));
                var ticket = service.getTicketDetail(userId, ticketId);
                writeJson(resp, HttpServletResponse.SC_OK, ApiResponse.ok(ticket));
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());

        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    private String getNormalizedPath(HttpServletRequest req) {
        String servletPath = req.getServletPath() != null ? req.getServletPath() : "";
        String pathInfo = req.getPathInfo() != null ? req.getPathInfo() : "";
        return servletPath + pathInfo;
    }

    private long getUserId(HttpServletRequest req) {
        Object uid = req.getAttribute("userId");
        if (uid instanceof Long l) return l;
        if (uid instanceof Integer i) return i.longValue();
        throw ApiException.unauthorized("Chưa đăng nhập hoặc thiếu token xác thực");
    }

    private void writeJson(HttpServletResponse resp, int statusCode, Object body) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json;charset=UTF-8");
        json.writeValue(resp.getWriter(), body);
    }
}
