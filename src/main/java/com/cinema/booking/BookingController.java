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

@WebServlet(name = "BookingController", urlPatterns = { "/bookings", "/bookings/*", "/booking", "/booking/*" })
public class BookingController extends BaseServlet {

    private final BookingService service;
    private final TicketService ticketService;
    private final PaymentService paymentService;

    public BookingController() {
        this.service = new BookingService();
        this.ticketService = new TicketService();
        this.paymentService = new PaymentService();
    }

    public BookingController(BookingService service, TicketService ticketService) {
        this(service, ticketService, new PaymentService());
    }

    public BookingController(BookingService service, TicketService ticketService, PaymentService paymentService) {
        this.service = service;
        this.ticketService = ticketService;
        this.paymentService = paymentService;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // 1. POST /bookings: Tạo booking & giữ ghế
        if (segments.length == 0) {
            CreateBookingRequest body = parseBody(req, CreateBookingRequest.class);
            CreateBookingResponse res = service.createBooking(userId, body);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            writeSuccess(resp, res);
            return;
        }

        if (segments.length == 2) {
            long bookingId = parsePathId(segments[0], "Mã đơn hàng");

            // Giai đoạn 2: POST /bookings/{bookingId}/payment
            if ("payment".equalsIgnoreCase(segments[1])) {
                CreatePaymentRequest body = parseBody(req, CreatePaymentRequest.class);
                var payment = paymentService.createPayment(userId, bookingId, body, req.getRemoteAddr());
                resp.setStatus(HttpServletResponse.SC_CREATED);
                writeSuccess(resp, payment);
                return;
            }

            // 5. POST /bookings/{bookingId}/cancel: Hủy booking
            if ("cancel".equalsIgnoreCase(segments[1])) {
                CancelBookingRequest body = null;
                try {
                    body = parseBody(req, CancelBookingRequest.class);
                } catch (Exception ignored) {
                }
                CancelBookingResponse res = service.cancelBooking(bookingId, userId, body);
                writeSuccess(resp, res);
                return;
            }

            // 6. POST /bookings/{bookingId}/refund: Yêu cầu hoàn tiền
            if ("refund".equalsIgnoreCase(segments[1])) {
                CreateRefundRequest body = null;
                try {
                    body = parseBody(req, CreateRefundRequest.class);
                } catch (Exception ignored) {
                }
                CreateRefundResponse res = service.createRefund(bookingId, userId, body);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                writeSuccess(resp, res);
                return;
            }
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // 2. GET /bookings/{bookingId}: Chi tiết booking
        if (segments.length == 1) {
            long bookingId = parsePathId(segments[0], "Mã đơn hàng");
            BookingDetailResponse res = service.getBookingDetail(bookingId, userId);
            writeSuccess(resp, res);
            return;
        }

        if (segments.length == 2) {
            long bookingId = parsePathId(segments[0], "Mã đơn hàng");

            // Giai đoạn 1: GET /bookings/{bookingId}/payment
            if ("payment".equalsIgnoreCase(segments[1])) {
                writeSuccess(resp, paymentService.getPaymentsByBooking(userId, bookingId));
                return;
            }

            // 7. GET /bookings/{bookingId}/refund: Xem tiến trình refund
            if ("refund".equalsIgnoreCase(segments[1])) {
                RefundDetailResponse res = service.getRefundDetail(bookingId, userId);
                writeSuccess(resp, res);
                return;
            }

            // GET /bookings/{bookingId}/tickets: Danh sách vé của booking
            if ("tickets".equalsIgnoreCase(segments[1])) {
                var tickets = ticketService.getTicketsByBooking(userId, bookingId);
                writeSuccess(resp, tickets);
                return;
            }
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        if (segments.length == 2) {
            long bookingId = parsePathId(segments[0], "Mã đơn hàng");

            // 3. PUT /bookings/{bookingId}/items: Cập nhật F&B
            if ("items".equalsIgnoreCase(segments[1])) {
                UpdateBookingItemsRequest body = parseBody(req, UpdateBookingItemsRequest.class);
                UpdateBookingItemsResponse res = service.updateBookingItems(bookingId, userId, body);
                writeSuccess(resp, res);
                return;
            }

            // 4. PUT /bookings/{bookingId}/voucher: Áp dụng voucher
            if ("voucher".equalsIgnoreCase(segments[1])) {
                ApplyVoucherRequest body = parseBody(req, ApplyVoucherRequest.class);
                ApplyVoucherResponse res = service.applyVoucher(bookingId, userId, body);
                writeSuccess(resp, res);
                return;
            }
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}
