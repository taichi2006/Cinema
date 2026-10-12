package com.cinema.booking;

import com.cinema.booking.dto.*;
import com.cinema.booking.entity.*;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.product.Product;
import com.cinema.product.ProductStatus;
import com.cinema.showtime.Showtime;
import com.cinema.showtime.ShowtimeSeat;
import com.cinema.voucher.Voucher;
import com.cinema.websocket.SeatSessionManager;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BookingDAO {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    // 1. POST /bookings: Tạo booking & giữ ghế
    public CreateBookingResponse createBooking(Long userId, Integer showTimeId, List<Integer> seatIds,
                                               List<CreateBookingRequest.ProductItem> productItems) {
        if (showTimeId == null) throw ApiException.badRequest("showTimeId không được để trống");
        if (seatIds == null || seatIds.isEmpty()) throw ApiException.badRequest("Danh sách ghế không được để trống");

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            List<ShowtimeSeat> seats = em.createQuery(
                    "SELECT s FROM ShowtimeSeat s WHERE s.showTimeId = :stId AND s.seatId IN :seatIds", ShowtimeSeat.class)
                    .setParameter("stId", showTimeId).setParameter("seatIds", seatIds)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();

            if (seats.size() != seatIds.size()) throw ApiException.conflict("Một hoặc nhiều ghế đã bị đặt hoặc không tồn tại");

            BigDecimal seatTotal = BigDecimal.ZERO;
            LocalDateTime now = LocalDateTime.now();
            for (ShowtimeSeat s : seats) {
                boolean isHeld = "SELECTED".equalsIgnoreCase(s.getStatus()) && s.getHoldExpirationAt() != null && s.getHoldExpirationAt().isAfter(now);
                if ("BOOKED".equalsIgnoreCase(s.getStatus()) || "UNAVAILABLE".equalsIgnoreCase(s.getStatus()) || isHeld) {
                    throw ApiException.conflict("Một hoặc nhiều ghế đã bị đặt");
                }
                seatTotal = seatTotal.add(s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO);
            }
            BigDecimal productTotal = BigDecimal.ZERO;
            Map<Long, Product> validProducts = new HashMap<>();
            if (productItems != null) {
                for (var item : productItems) {
                    if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) continue;
                    Product prod = em.find(Product.class, item.getProductId().longValue());
                    if (prod == null) throw ApiException.unprocessable("Sản phẩm F&B không tồn tại: " + item.getProductId());
                    if (prod.getStatus() != ProductStatus.AVAILABLE) throw ApiException.unprocessable("Sản phẩm F&B hiện không khả dụng: " + item.getProductId());
                    productTotal = productTotal.add(prod.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                    validProducts.put(prod.getProductId(), prod);
                }
            }
            BigDecimal grandTotal = seatTotal.add(productTotal);
            LocalDateTime holdExpiresAt = now.plusMinutes(10);
            Booking booking = new Booking();
            booking.setUserId(userId);
            booking.setStatus("PENDING");
            booking.setTotalAmount(grandTotal);
            booking.setExpiredAt(holdExpiresAt);
            em.persist(booking);
            em.flush();

            for (ShowtimeSeat s : seats) {
                s.setStatus("SELECTED");
                s.setBookingId(booking.getBookingId());
                s.setHoldExpirationAt(holdExpiresAt);
            }
            if (productItems != null) {
                for (var item : productItems) {
                    if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) continue;
                    Product prod = validProducts.get(item.getProductId().longValue());
                    BigDecimal subtotal = prod.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                    em.persist(new BookingItem(booking, item.getProductId(), item.getQuantity(), prod.getPrice(), subtotal));
                }
            }

            em.getTransaction().commit();

            try {
                SeatSessionManager.getInstance().markBooked(
                        showTimeId.longValue(),
                        seatIds.stream().map(Integer::longValue).toList()
                );
            } catch (Exception ignored) {}

            return new CreateBookingResponse(booking.getBookingId(), showTimeId, seatIds, grandTotal, holdExpiresAt.format(ISO_FORMATTER), "PENDING");
        } catch (ApiException e) {
            rollbackQuietly(em);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(em);
            throw ApiException.internal("Lỗi tạo booking: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    // 2. GET /bookings/{bookingId}: Chi tiết booking
    public BookingDetailResponse getBookingDetail(Long bookingId, Long userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Booking booking = em.find(Booking.class, bookingId);
            if (booking == null) throw ApiException.notFound("Không tìm thấy đơn đặt vé");
            if (userId != null && !booking.getUserId().equals(userId)) throw ApiException.forbidden("Bạn không có quyền truy cập đơn hàng này");

            if ("PENDING".equalsIgnoreCase(booking.getStatus()) && booking.getExpiredAt() != null && booking.getExpiredAt().isBefore(LocalDateTime.now())) {
                List<Object[]> expiredSeats = em.createQuery(
                        "SELECT s.showTimeId, s.seatId FROM ShowtimeSeat s WHERE s.bookingId = :bId", Object[].class)
                        .setParameter("bId", bookingId).getResultList();
                em.getTransaction().begin();
                booking.setStatus("EXPIRED");
                em.createQuery("UPDATE ShowtimeSeat s SET s.status = 'AVAILABLE', s.bookingId = null, s.holdExpirationAt = null WHERE s.bookingId = :bId")
                        .setParameter("bId", bookingId).executeUpdate();
                em.getTransaction().commit();

                try {
                    for (Object[] r : expiredSeats) {
                        Long stId = ((Number) r[0]).longValue();
                        Long sId = ((Number) r[1]).longValue();
                        SeatSessionManager.getInstance().releaseBookedSeat(stId, sId);
                    }
                } catch (Exception ignored) {}
            }

            List<Object[]> stRows = em.createQuery(
                    "SELECT st, m.title FROM ShowtimeSeat sts, Showtime st, Movie m " +
                    "WHERE sts.showTimeId = st.showTimeId AND st.movieId = m.movieId AND sts.bookingId = :bId", Object[].class)
                    .setParameter("bId", bookingId).setMaxResults(1).getResultList();
            BookingDetailResponse.ShowTimeSummary showTimeSummary = null;
            if (!stRows.isEmpty()) {
                Showtime st = (Showtime) stRows.get(0)[0];
                showTimeSummary = new BookingDetailResponse.ShowTimeSummary(st.getShowTimeId(), (String) stRows.get(0)[1],
                        LocalDateTime.of(st.getShowDate(), st.getStartTime()).format(ISO_FORMATTER));
            }

            List<String> seats = em.createQuery(
                    "SELECT COALESCE(s.seatLabel, CONCAT(s.seatRow, s.seatCol)) FROM ShowtimeSeat sts, Seat s " +
                    "WHERE sts.seatId = s.seatId AND sts.bookingId = :bId ORDER BY s.seatRow, s.seatCol", String.class)
                    .setParameter("bId", bookingId).getResultList();

            List<Object[]> prodRows = em.createQuery(
                    "SELECT p.productName, bi.quantity, bi.price FROM BookingItem bi, Product p " +
                    "WHERE bi.productId = p.productId AND bi.booking.bookingId = :bId ORDER BY bi.bookingItemId", Object[].class)
                    .setParameter("bId", bookingId).getResultList();

            List<BookingDetailResponse.ProductSummary> products = new ArrayList<>();
            for (Object[] r : prodRows) products.add(new BookingDetailResponse.ProductSummary((String) r[0], (Integer) r[1], (BigDecimal) r[2]));

            return new BookingDetailResponse(booking.getBookingId(), showTimeSummary, seats, products, booking.getTotalAmount(), booking.getStatus());
        } finally {
            em.close();
        }
    }

    // 3. PUT /bookings/{bookingId}/items: Cập nhật F&B
    public UpdateBookingItemsResponse updateBookingItems(Long bookingId, Long userId, List<UpdateBookingItemsRequest.Item> items) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Booking booking = em.find(Booking.class, bookingId);
            if (booking == null) throw ApiException.notFound("Không tìm thấy đơn đặt vé");
            if (userId != null && !booking.getUserId().equals(userId)) throw ApiException.forbidden("Bạn không có quyền chỉnh sửa đơn hàng này");

            LocalDateTime now = LocalDateTime.now();
            if (!"PENDING".equalsIgnoreCase(booking.getStatus()) || (booking.getExpiredAt() != null && booking.getExpiredAt().isBefore(now))) {
                throw ApiException.conflict("Đã hết hạn giữ ghế hoặc đơn không ở trạng thái cho phép");
            }

            em.createQuery("DELETE FROM BookingItem bi WHERE bi.booking.bookingId = :bId").setParameter("bId", bookingId).executeUpdate();

            BigDecimal seatSubtotal = em.createQuery(
                    "SELECT COALESCE(SUM(s.price), 0) FROM ShowtimeSeat s WHERE s.bookingId = :bId", BigDecimal.class)
                    .setParameter("bId", bookingId).getSingleResult();

            BigDecimal newFnbTotal = BigDecimal.ZERO;
            List<UpdateBookingItemsResponse.ProductItemResponse> prodResponses = new ArrayList<>();

            if (items != null) {
                for (var it : items) {
                    if (it.getProductId() == null || it.getQuantity() == null || it.getQuantity() <= 0) continue;
                    Product prod = em.find(Product.class, it.getProductId().longValue());
                    if (prod == null) throw ApiException.unprocessable("Sản phẩm không tồn tại: " + it.getProductId());
                    BigDecimal subtotal = prod.getPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
                    em.persist(new BookingItem(booking, it.getProductId(), it.getQuantity(), prod.getPrice(), subtotal));
                    newFnbTotal = newFnbTotal.add(subtotal);
                    prodResponses.add(new UpdateBookingItemsResponse.ProductItemResponse(it.getProductId(), it.getQuantity(), prod.getPrice()));
                }
            }

            BigDecimal discount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal newTotal = seatSubtotal.add(newFnbTotal).subtract(discount).max(BigDecimal.ZERO);
            booking.setTotalAmount(newTotal);
            booking.setUpdatedAt(now);
            em.getTransaction().commit();
            return new UpdateBookingItemsResponse(bookingId, newTotal, prodResponses);
        } catch (ApiException e) {
            rollbackQuietly(em);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(em);
            throw ApiException.internal("Lỗi cập nhật F&B: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    // 4. PUT /bookings/{bookingId}/voucher: Áp dụng voucher
    public ApplyVoucherResponse applyVoucher(Long bookingId, Long userId, String code) {
        if (code == null || code.trim().isEmpty()) throw ApiException.badRequest("Mã voucher không được để trống");
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Booking booking = em.find(Booking.class, bookingId);
            if (booking == null) throw ApiException.notFound("Không tìm thấy đơn đặt vé");
            if (userId != null && !booking.getUserId().equals(userId)) throw ApiException.forbidden("Bạn không có quyền áp dụng voucher cho đơn hàng này");

            LocalDateTime now = LocalDateTime.now();
            if (!"PENDING".equalsIgnoreCase(booking.getStatus()) || (booking.getExpiredAt() != null && booking.getExpiredAt().isBefore(now))) {
                throw ApiException.conflict("Đã hết hạn giữ ghế hoặc đơn không ở trạng thái cho phép");
            }

            List<Voucher> vouchers = em.createQuery("SELECT v FROM Voucher v WHERE v.code = :code", Voucher.class)
                    .setParameter("code", code.trim()).getResultList();
            if (vouchers.isEmpty()) throw ApiException.unprocessable("Voucher không hợp lệ hoặc đã hết lượt dùng");

            Voucher v = vouchers.get(0);
            LocalDate today = LocalDate.now();
            boolean isInvalid = !"ACTIVE".equalsIgnoreCase(v.getStatus())
                    || (v.getStartDate() != null && today.isBefore(v.getStartDate()))
                    || (v.getEndDate() != null && today.isAfter(v.getEndDate()))
                    || (v.getQuantity() != null && v.getQuantity() > 0 && v.getUsedCount() != null && v.getUsedCount() >= v.getQuantity());
            if (isInvalid) throw ApiException.unprocessable("Voucher không hợp lệ hoặc đã hết lượt dùng");
            BigDecimal seatSubtotal = em.createQuery("SELECT COALESCE(SUM(s.price), 0) FROM ShowtimeSeat s WHERE s.bookingId = :bId", BigDecimal.class)
                    .setParameter("bId", bookingId).getSingleResult();
            BigDecimal prodSubtotal = em.createQuery("SELECT COALESCE(SUM(bi.subtotal), 0) FROM BookingItem bi WHERE bi.booking.bookingId = :bId", BigDecimal.class)
                    .setParameter("bId", bookingId).getSingleResult();
            BigDecimal orderSubtotal = seatSubtotal.add(prodSubtotal);

            BigDecimal minOrder = v.getMinOrderAmount() != null ? v.getMinOrderAmount() : BigDecimal.ZERO;
            if (orderSubtotal.compareTo(minOrder) < 0) {
                throw ApiException.unprocessable("Đơn hàng chưa đạt giá trị tối thiểu " + minOrder.longValue() + "đ để dùng voucher");
            }

            BigDecimal discount = BigDecimal.ZERO;
            if ("PERCENTAGE".equalsIgnoreCase(v.getDiscountType())) {
                discount = orderSubtotal.multiply(v.getDiscountValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (v.getMaxDiscount() != null && discount.compareTo(v.getMaxDiscount()) > 0) discount = v.getMaxDiscount();
            } else {
                discount = v.getDiscountValue();
            }
            if (discount.compareTo(orderSubtotal) > 0) discount = orderSubtotal;

            BigDecimal finalAmount = orderSubtotal.subtract(discount).max(BigDecimal.ZERO);
            booking.setVoucherId(v.getVoucherId().longValue());
            booking.setVoucherCode(v.getCode());
            booking.setDiscountAmount(discount);
            booking.setTotalAmount(finalAmount);
            booking.setUpdatedAt(now);
            em.getTransaction().commit();
            return new ApplyVoucherResponse(bookingId, discount, finalAmount, v.getCode());
        } catch (ApiException e) {
            rollbackQuietly(em);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(em);
            throw ApiException.internal("Lỗi áp dụng voucher: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    // 5. POST /bookings/{bookingId}/cancel: Hủy booking
    public CancelBookingResponse cancelBooking(Long bookingId, Long userId, String reason) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();
            Booking booking = em.find(Booking.class, bookingId);
            if (booking == null) throw ApiException.notFound("Không tìm thấy đơn đặt vé");
            if (userId != null && !booking.getUserId().equals(userId)) throw ApiException.forbidden("Bạn không có quyền hủy đơn hàng này");
            if (!"PENDING".equalsIgnoreCase(booking.getStatus())) throw ApiException.conflict("Chỉ hủy được đơn chưa thanh toán hoặc đang chờ thanh toán");

            LocalDateTime now = LocalDateTime.now();
            List<Object[]> cancelledSeats = em.createQuery(
                    "SELECT s.showTimeId, s.seatId FROM ShowtimeSeat s WHERE s.bookingId = :bId", Object[].class)
                    .setParameter("bId", bookingId).getResultList();

            em.createQuery("UPDATE ShowtimeSeat s SET s.status = 'AVAILABLE', s.bookingId = null, s.holdExpirationAt = null WHERE s.bookingId = :bId")
                    .setParameter("bId", bookingId).executeUpdate();

            booking.setStatus("CANCELLED");
            booking.setCancelledAt(now);
            booking.setCancelReason(reason != null ? reason : "Người dùng yêu cầu hủy");
            booking.setUpdatedAt(now);
            em.getTransaction().commit();

            try {
                for (Object[] r : cancelledSeats) {
                    Long stId = ((Number) r[0]).longValue();
                    Long sId = ((Number) r[1]).longValue();
                    SeatSessionManager.getInstance().releaseBookedSeat(stId, sId);
                }
            } catch (Exception ignored) {}

            return new CancelBookingResponse(bookingId, "CANCELLED", now.format(ISO_FORMATTER));
        } catch (ApiException e) {
            rollbackQuietly(em);
            throw e;
        } catch (Exception e) {
            rollbackQuietly(em);
            throw ApiException.internal("Lỗi hủy đơn đặt vé: " + e.getMessage());
        } finally {
            em.close();
        }
    }
    private void rollbackQuietly(EntityManager em) {
        if (em.getTransaction().isActive()) em.getTransaction().rollback();
    }
}
