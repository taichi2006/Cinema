package com.cinema.booking;

import com.cinema.booking.dto.*;
import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.common.exception.ErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/bookings", "/bookings/*", "/booking", "/booking/*"})
public class BookingController extends HttpServlet {

    private final BookingService service = new BookingService();
    private final ObjectMapper json = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = normalizePath(req.getPathInfo());

        try {
            Integer userId = getAuthenticatedUserId(req);

            // 1. POST /bookings (Tạo booking & giữ ghế)
            if (path.isEmpty() || path.equals("/")) {
                CreateBookingRequest body = json.readValue(req.getInputStream(), CreateBookingRequest.class);
                CreateBookingResponse res = service.createBooking(userId, body);

                resp.setStatus(HttpServletResponse.SC_CREATED);
                json.writeValue(resp.getWriter(), ApiResponse.created(res));
                return;
            }

            // 5. POST /bookings/{bookingId}/cancel (Hủy booking)
            if (path.matches("^/\\d+/cancel/?$")) {
                Integer bookingId = extractIdFromPath(path);
                CancelBookingRequest body = null;
                try {
                    body = json.readValue(req.getInputStream(), CancelBookingRequest.class);
                } catch (Exception ignored) {}

                CancelBookingResponse res = service.cancelBooking(bookingId, userId, body);
                resp.setStatus(HttpServletResponse.SC_OK);
                json.writeValue(resp.getWriter(), ApiResponse.ok(res));
                return;
            }

            // 6. POST /bookings/{bookingId}/refund (Yêu cầu hoàn tiền)
            if (path.matches("^/\\d+/refund/?$")) {
                Integer bookingId = extractIdFromPath(path);
                CreateRefundRequest body = null;
                try {
                    body = json.readValue(req.getInputStream(), CreateRefundRequest.class);
                } catch (Exception ignored) {}

                CreateRefundResponse res = service.createRefund(bookingId, userId, body);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                json.writeValue(resp.getWriter(), ApiResponse.created(res));
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại");

        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = normalizePath(req.getPathInfo());

        try {
            Integer userId = getAuthenticatedUserId(req);

            // 7. GET /bookings/{bookingId}/refund (Xem tiến trình refund)
            if (path.matches("^/\\d+/refund/?$")) {
                Integer bookingId = extractIdFromPath(path);
                RefundDetailResponse res = service.getRefundDetail(bookingId, userId);

                resp.setStatus(HttpServletResponse.SC_OK);
                json.writeValue(resp.getWriter(), ApiResponse.ok(res));
                return;
            }

            // 2. GET /bookings/{bookingId} (Chi tiết booking)
            if (path.matches("^/\\d+/?$")) {
                Integer bookingId = extractIdFromPath(path);
                BookingDetailResponse res = service.getBookingDetail(bookingId, userId);

                resp.setStatus(HttpServletResponse.SC_OK);
                json.writeValue(resp.getWriter(), ApiResponse.ok(res));
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại");

        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String path = normalizePath(req.getPathInfo());

        try {
            Integer userId = getAuthenticatedUserId(req);

            // 3. PUT /bookings/{bookingId}/items (Cập nhật F&B)
            if (path.matches("^/\\d+/items/?$")) {
                Integer bookingId = extractIdFromPath(path);
                UpdateBookingItemsRequest body = json.readValue(req.getInputStream(), UpdateBookingItemsRequest.class);
                UpdateBookingItemsResponse res = service.updateBookingItems(bookingId, userId, body);

                resp.setStatus(HttpServletResponse.SC_OK);
                json.writeValue(resp.getWriter(), ApiResponse.ok(res));
                return;
            }

            // 4. PUT /bookings/{bookingId}/voucher (Áp dụng voucher)
            if (path.matches("^/\\d+/voucher/?$")) {
                Integer bookingId = extractIdFromPath(path);
                ApplyVoucherRequest body = json.readValue(req.getInputStream(), ApplyVoucherRequest.class);
                ApplyVoucherResponse res = service.applyVoucher(bookingId, userId, body);

                resp.setStatus(HttpServletResponse.SC_OK);
                json.writeValue(resp.getWriter(), ApiResponse.ok(res));
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại");

        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    private String normalizePath(String pathInfo) {
        if (pathInfo == null || pathInfo.trim().isEmpty()) return "";
        return pathInfo.trim();
    }

    private Integer extractIdFromPath(String path) {
        String[] parts = path.split("/");
        for (String p : parts) {
            if (p.matches("\\d+")) {
                return Integer.parseInt(p);
            }
        }
        throw ApiException.badRequest("ID đơn hàng không hợp lệ");
    }

    private Integer getAuthenticatedUserId(HttpServletRequest req) {
        Object userAttr = req.getAttribute("userId");
        if (userAttr instanceof Integer i) return i;
        if (userAttr instanceof Number n) return n.intValue();
        if (userAttr instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return 1; // Default fallback for development/testing
    }
}
