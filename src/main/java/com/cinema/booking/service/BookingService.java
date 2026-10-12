package com.cinema.booking.service;

import com.cinema.booking.dao.BookingDAO;
import com.cinema.booking.dao.RefundDAO;
import com.cinema.booking.dto.request.*;
import com.cinema.booking.dto.response.*;
import com.cinema.common.exception.ApiException;

public class BookingService {

    private final BookingDAO bookingDAO = new BookingDAO();
    private final RefundDAO refundDAO = new RefundDAO();

    // 1. Tạo đơn đặt vé & giữ ghế
    public CreateBookingResponse createBooking(Long userId, CreateBookingRequest req) {
        if (req == null || req.getShowTimeId() == null || req.getSeatIds() == null || req.getSeatIds().isEmpty()) {
            throw ApiException.badRequest("Dữ liệu đặt vé không hợp lệ");
        }
        return bookingDAO.createBooking(userId, req.getShowTimeId(), req.getSeatIds(), req.getProductItems());
    }

    // 2. Chi tiết đơn đặt vé
    public BookingDetailResponse getBookingDetail(Long bookingId, Long userId) {
        validateBookingId(bookingId);
        return bookingDAO.getBookingDetail(bookingId, userId);
    }

    // 3. Cập nhật bắp nước (F&B)
    public UpdateBookingItemsResponse updateBookingItems(Long bookingId, Long userId, UpdateBookingItemsRequest req) {
        validateBookingId(bookingId);
        if (req == null || req.getItems() == null) throw ApiException.badRequest("Dữ liệu bắp nước (F&B) không hợp lệ");
        return bookingDAO.updateBookingItems(bookingId, userId, req.getItems());
    }

    // 4. Áp dụng mã giảm giá (Voucher)
    public ApplyVoucherResponse applyVoucher(Long bookingId, Long userId, ApplyVoucherRequest req) {
        validateBookingId(bookingId);
        if (req == null || req.getCode() == null || req.getCode().trim().isEmpty()) {
            throw ApiException.badRequest("Mã voucher không được để trống");
        }
        return bookingDAO.applyVoucher(bookingId, userId, req.getCode().trim());
    }

    // 5. Hủy đơn đặt vé
    public CancelBookingResponse cancelBooking(Long bookingId, Long userId, CancelBookingRequest req) {
        validateBookingId(bookingId);
        String reason = (req != null && req.getReason() != null && !req.getReason().isBlank()) ? req.getReason().trim() : "Khách hàng hủy đơn";
        return bookingDAO.cancelBooking(bookingId, userId, reason);
    }

    // 6. Yêu cầu hoàn tiền
    public CreateRefundResponse createRefund(Long bookingId, Long userId, CreateRefundRequest req) {
        validateBookingId(bookingId);
        String reason = (req != null && req.getReason() != null && !req.getReason().isBlank()) ? req.getReason().trim() : "Yêu cầu hoàn tiền vé";
        return refundDAO.createRefund(bookingId, userId, reason);
    }

    // 7. Xem trạng thái hoàn tiền
    public RefundDetailResponse getRefundDetail(Long bookingId, Long userId) {
        validateBookingId(bookingId);
        return refundDAO.getRefundDetail(bookingId, userId);
    }

    private void validateBookingId(Long bookingId) {
        if (bookingId == null || bookingId <= 0) {
            throw ApiException.badRequest("Mã đơn hàng không hợp lệ");
        }
    }
}
