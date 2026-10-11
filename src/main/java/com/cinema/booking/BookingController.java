package com.cinema.booking;

import com.cinema.booking.dto.*;
import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import com.cinema.payment.dto.CreatePaymentRequest;
import com.cinema.payment.PaymentService;
import com.cinema.ticket.TicketService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/bookings", "/bookings/*"})
public class BookingController extends BaseServlet {

    private final BookingService service = new BookingService();
    private final TicketService ticketService = new TicketService();
    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // POST /bookings (Tạo booking)
        if (segments.length == 0) {
            CreateBookingRequest body = parseBody(req, CreateBookingRequest.class);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            writeSuccess(resp, service.createBooking(userId, body));
            return;
        }

        // POST /bookings/{id}/{action} (cancel / refund)
        if (segments.length == 2) {
            long bookingId = parsePathId(segments[0], "Mã đơn hàng");
            String action = segments[1].toLowerCase();

            if ("payment".equals(action)) {
                CreatePaymentRequest body = parseBody(req, CreatePaymentRequest.class);
                var payment = paymentService.createPayment(userId, bookingId, body, req.getRemoteAddr());
                resp.setStatus(HttpServletResponse.SC_CREATED);
                writeSuccess(resp, payment);
                return;
            }

            if ("cancel".equals(action)) {
                CancelBookingRequest body = parseOptionalBody(req, CancelBookingRequest.class);
                writeSuccess(resp, service.cancelBooking(bookingId, userId, body));
                return;
            }

            if ("refund".equals(action)) {
                CreateRefundRequest body = parseOptionalBody(req, CreateRefundRequest.class);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                writeSuccess(resp, service.createRefund(bookingId, userId, body));
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại: " + action);
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // GET /bookings/{id} (Chi tiết)
        if (segments.length == 1) {
            long bookingId = parsePathId(segments[0], "Mã đơn hàng");
            writeSuccess(resp, service.getBookingDetail(bookingId, userId));
            return;
        }

        // GET /bookings/{id}/{sub} (refund / tickets)
        if (segments.length == 2) {
            long bookingId = parsePathId(segments[0], "Mã đơn hàng");
            String sub = segments[1].toLowerCase();

            if ("payment".equals(sub)) {
                writeSuccess(resp, paymentService.getPaymentsByBooking(userId, bookingId));
                return;
            }

            if ("refund".equals(sub)) {
                writeSuccess(resp, service.getRefundDetail(bookingId, userId));
                return;
            }

            if ("tickets".equals(sub)) {
                writeSuccess(resp, ticketService.getTicketsByBooking(userId, bookingId));
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại: " + sub);
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // PUT /bookings/{id}/{action} (items / voucher)
        if (segments.length == 2) {
            long bookingId = parsePathId(segments[0], "Mã đơn hàng");
            String action = segments[1].toLowerCase();

            if ("items".equals(action)) {
                UpdateBookingItemsRequest body = parseBody(req, UpdateBookingItemsRequest.class);
                writeSuccess(resp, service.updateBookingItems(bookingId, userId, body));
                return;
            }

            if ("voucher".equals(action)) {
                ApplyVoucherRequest body = parseBody(req, ApplyVoucherRequest.class);
                writeSuccess(resp, service.applyVoucher(bookingId, userId, body));
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại: " + action);
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}
