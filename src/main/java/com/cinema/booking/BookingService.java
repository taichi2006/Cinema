package com.cinema.booking;

import com.cinema.booking.dto.*;
import com.cinema.common.exception.ApiException;

public class BookingService {

    private final BookingDAO bookingDAO;

    public BookingService() {
        this.bookingDAO = new BookingDAO();
    }

    public BookingService(BookingDAO bookingDAO) {
        this.bookingDAO = bookingDAO;
    }

    // 1. POST /bookings: Tạo booking & giữ ghế
    public CreateBookingResponse createBooking(Integer userId, CreateBookingRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Dữ liệu yêu cầu không hợp lệ");
        }
        if (request.getShowTimeId() == null) {
            throw ApiException.badRequest("showTimeId không được để trống");
        }
        if (request.getSeatIds() == null || request.getSeatIds().isEmpty()) {
            throw ApiException.badRequest("Danh sách ghế không được để trống");
        }
        return bookingDAO.createBooking(userId, request.getShowTimeId(), request.getSeatIds(), request.getProductItems());
    }

    // 2. GET /bookings/{bookingId}: Chi tiết booking
    public BookingDetailResponse getBookingDetail(Integer bookingId, Integer userId) {
        if (bookingId == null) {
            throw ApiException.badRequest("bookingId không hợp lệ");
        }
        return bookingDAO.getBookingDetail(bookingId, userId);
    }

    // 3. PUT /bookings/{bookingId}/items: Cập nhật F&B
    public UpdateBookingItemsResponse updateBookingItems(Integer bookingId, Integer userId, UpdateBookingItemsRequest request) {
        if (bookingId == null) {
            throw ApiException.badRequest("bookingId không hợp lệ");
        }
        if (request == null) {
            throw ApiException.badRequest("Dữ liệu yêu cầu không hợp lệ");
        }
        return bookingDAO.updateBookingItems(bookingId, userId, request.getItems());
    }

    // 4. PUT /bookings/{bookingId}/voucher: Áp dụng voucher
    public ApplyVoucherResponse applyVoucher(Integer bookingId, Integer userId, ApplyVoucherRequest request) {
        if (bookingId == null) {
            throw ApiException.badRequest("bookingId không hợp lệ");
        }
        if (request == null || request.getCode() == null || request.getCode().trim().isEmpty()) {
            throw ApiException.badRequest("Mã voucher không được để trống");
        }
        return bookingDAO.applyVoucher(bookingId, userId, request.getCode().trim());
    }

    // 5. POST /bookings/{bookingId}/cancel: Hủy booking
    public CancelBookingResponse cancelBooking(Integer bookingId, Integer userId, CancelBookingRequest request) {
        if (bookingId == null) {
            throw ApiException.badRequest("bookingId không hợp lệ");
        }
        String reason = (request != null && request.getReason() != null) ? request.getReason() : "Khách hàng hủy đơn";
        return bookingDAO.cancelBooking(bookingId, userId, reason);
    }

    // 6. POST /bookings/{bookingId}/refund: Yêu cầu hoàn tiền
    public CreateRefundResponse createRefund(Integer bookingId, Integer userId, CreateRefundRequest request) {
        if (bookingId == null) {
            throw ApiException.badRequest("bookingId không hợp lệ");
        }
        String reason = (request != null && request.getReason() != null) ? request.getReason() : "Yêu cầu hoàn tiền vé";
        return bookingDAO.createRefund(bookingId, userId, reason);
    }

    // 7. GET /bookings/{bookingId}/refund: Xem tiến trình refund
    public RefundDetailResponse getRefundDetail(Integer bookingId, Integer userId) {
        if (bookingId == null) {
            throw ApiException.badRequest("bookingId không hợp lệ");
        }
        return bookingDAO.getRefundDetail(bookingId, userId);
    }
}
