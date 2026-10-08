package com.cinema.booking;

import com.cinema.booking.dto.*;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.BookingItem;
import com.cinema.booking.entity.Refund;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BookingDAO {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // 1. POST /bookings: Tạo booking & giữ ghế
    public CreateBookingResponse createBooking(Integer userId, Integer showTimeId, List<Integer> seatIds,
                                               List<CreateBookingRequest.ProductItem> productItems) {
        if (showTimeId == null) {
            throw ApiException.badRequest("showTimeId không được để trống");
        }
        if (seatIds == null || seatIds.isEmpty()) {
            throw ApiException.badRequest("Danh sách ghế không được để trống");
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            // 1.1 Verify and lock seats for this showtime
            String checkSeatSql = """
                SELECT id, seat_id, price, status, hold_expiration_at
                FROM cinema.show_time_seats
                WHERE show_time_id = :showTimeId AND seat_id IN (:seatIds)
                FOR UPDATE
            """;
            Query seatQuery = em.createNativeQuery(checkSeatSql);
            seatQuery.setParameter("showTimeId", showTimeId);
            seatQuery.setParameter("seatIds", seatIds);

            List<?> seatRows = seatQuery.getResultList();
            if (seatRows.size() != seatIds.size()) {
                throw ApiException.conflict("Một hoặc nhiều ghế đã bị đặt hoặc không tồn tại");
            }

            BigDecimal seatTotal = BigDecimal.ZERO;
            LocalDateTime now = LocalDateTime.now();

            for (Object obj : seatRows) {
                Object[] row = (Object[]) obj;
                String status = (String) row[3];
                Timestamp holdExp = (Timestamp) row[4];
                BigDecimal price = new BigDecimal(row[2].toString());

                if ("BOOKED".equalsIgnoreCase(status) || "UNAVAILABLE".equalsIgnoreCase(status)) {
                    throw ApiException.conflict("Một hoặc nhiều ghế đã bị đặt");
                }
                if ("SELECTED".equalsIgnoreCase(status) && holdExp != null && holdExp.toLocalDateTime().isAfter(now)) {
                    throw ApiException.conflict("Một hoặc nhiều ghế đã bị đặt");
                }

                seatTotal = seatTotal.add(price);
            }

            // 1.2 Verify products & calculate product subtotal
            BigDecimal productTotal = BigDecimal.ZERO;
            Map<Integer, Object[]> validProducts = new HashMap<>();

            if (productItems != null && !productItems.isEmpty()) {
                for (CreateBookingRequest.ProductItem item : productItems) {
                    if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                        continue;
                    }
                    String prodSql = "SELECT product_id, product_name, price, status FROM cinema.products WHERE product_id = :prodId";
                    Query prodQuery = em.createNativeQuery(prodSql);
                    prodQuery.setParameter("prodId", item.getProductId());
                    List<?> prodList = prodQuery.getResultList();
                    if (prodList.isEmpty()) {
                        throw ApiException.unprocessable("Sản phẩm F&B không tồn tại: " + item.getProductId());
                    }
                    Object[] prodRow = (Object[]) prodList.get(0);
                    String prodStatus = (String) prodRow[3];
                    if (!"AVAILABLE".equalsIgnoreCase(prodStatus) && !"ACTIVE".equalsIgnoreCase(prodStatus)) {
                        throw ApiException.unprocessable("Sản phẩm F&B hiện không khả dụng: " + item.getProductId());
                    }
                    BigDecimal prodPrice = new BigDecimal(prodRow[2].toString());
                    productTotal = productTotal.add(prodPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
                    validProducts.put(item.getProductId(), prodRow);
                }
            }

            BigDecimal grandTotal = seatTotal.add(productTotal);
            LocalDateTime holdExpiresAt = now.plusMinutes(10);

            // 1.3 Insert Booking
            Booking booking = new Booking();
            booking.setUserId(userId);
            booking.setStatus("PENDING");
            booking.setTotalAmount(grandTotal);
            booking.setExpiredAt(holdExpiresAt);
            booking.setCreatedAt(now);
            booking.setUpdatedAt(now);
            em.persist(booking);
            em.flush(); // populate generated booking_id

            // 1.4 Hold seats: update show_time_seats
            String holdSeatsSql = """
                UPDATE cinema.show_time_seats
                SET status = 'SELECTED',
                    booking_id = :bookingId,
                    hold_expiration_at = :holdExp,
                    version = version + 1
                WHERE show_time_id = :showTimeId AND seat_id IN (:seatIds)
            """;
            Query updateSeatsQuery = em.createNativeQuery(holdSeatsSql);
            updateSeatsQuery.setParameter("bookingId", booking.getBookingId());
            updateSeatsQuery.setParameter("holdExp", Timestamp.valueOf(holdExpiresAt));
            updateSeatsQuery.setParameter("showTimeId", showTimeId);
            updateSeatsQuery.setParameter("seatIds", seatIds);
            updateSeatsQuery.executeUpdate();

            // 1.5 Insert booking items
            if (productItems != null && !productItems.isEmpty()) {
                for (CreateBookingRequest.ProductItem item : productItems) {
                    if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) continue;
                    Object[] prodRow = validProducts.get(item.getProductId());
                    BigDecimal price = new BigDecimal(prodRow[2].toString());
                    BigDecimal subtotal = price.multiply(BigDecimal.valueOf(item.getQuantity()));

                    BookingItem bItem = new BookingItem(booking, item.getProductId(), item.getQuantity(), price, subtotal);
                    em.persist(bItem);
                }
            }

            em.getTransaction().commit();

            return new CreateBookingResponse(
                    booking.getBookingId(),
                    showTimeId,
                    seatIds,
                    grandTotal,
                    holdExpiresAt.format(ISO_FORMATTER),
                    "PENDING"
            );
        } catch (ApiException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ApiException.internal("Lỗi tạo booking: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    // 2. GET /bookings/{bookingId}: Chi tiết booking
    public BookingDetailResponse getBookingDetail(Integer bookingId, Integer userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Booking booking = em.find(Booking.class, bookingId);
            if (booking == null) {
                throw ApiException.notFound("Không tìm thấy đơn đặt vé");
            }
            if (userId != null && !booking.getUserId().equals(userId)) {
                throw ApiException.forbidden("Bạn không có quyền truy cập đơn hàng này");
            }

            // Check if expired
            LocalDateTime now = LocalDateTime.now();
            if ("PENDING".equalsIgnoreCase(booking.getStatus()) && booking.getExpiredAt() != null
                    && booking.getExpiredAt().isBefore(now)) {
                em.getTransaction().begin();
                booking.setStatus("EXPIRED");
                // Release seats
                Query releaseQ = em.createNativeQuery("""
                    UPDATE cinema.show_time_seats
                    SET status = 'AVAILABLE', booking_id = NULL, hold_expiration_at = NULL
                    WHERE booking_id = :bId AND status = 'SELECTED'
                """);
                releaseQ.setParameter("bId", bookingId);
                releaseQ.executeUpdate();
                em.getTransaction().commit();
            }

            // Query showtime & movie info
            String showtimeSql = """
                SELECT st.show_time_id, m.title, st.show_date, st.start_time
                FROM cinema.show_time_seats sts
                JOIN cinema.show_times st ON sts.show_time_id = st.show_time_id
                JOIN cinema.movies m ON st.movie_id = m.movie_id
                WHERE sts.booking_id = :bId
                LIMIT 1
            """;
            Query stQuery = em.createNativeQuery(showtimeSql);
            stQuery.setParameter("bId", bookingId);
            List<?> stList = stQuery.getResultList();

            BookingDetailResponse.ShowTimeSummary showTimeSummary = null;
            if (!stList.isEmpty()) {
                Object[] r = (Object[]) stList.get(0);
                Integer stId = ((Number) r[0]).intValue();
                String movieTitle = (String) r[1];
                LocalDate sDate = r[2] instanceof Date ? ((Date) r[2]).toLocalDate() : (LocalDate) r[2];
                LocalTime sTime = r[3] instanceof Time ? ((Time) r[3]).toLocalTime() : (LocalTime) r[3];
                LocalDateTime startDt = LocalDateTime.of(sDate, sTime);
                showTimeSummary = new BookingDetailResponse.ShowTimeSummary(stId, movieTitle, startDt.format(ISO_FORMATTER));
            }

            // Query seats
            String seatsSql = """
                SELECT COALESCE(s.seat_label, CONCAT(s.seat_row, s.seat_col))
                FROM cinema.show_time_seats sts
                JOIN cinema.seats s ON sts.seat_id = s.seat_id
                WHERE sts.booking_id = :bId
                ORDER BY s.seat_row, s.seat_col
            """;
            Query seatsQ = em.createNativeQuery(seatsSql);
            seatsQ.setParameter("bId", bookingId);
            List<?> seatRows = seatsQ.getResultList();
            List<String> seats = new ArrayList<>();
            for (Object s : seatRows) {
                seats.add(s != null ? s.toString() : "");
            }

            // Query products
            String prodSql = """
                SELECT p.product_name, bi.quantity, bi.price
                FROM cinema.booking_items bi
                JOIN cinema.products p ON bi.product_id = p.product_id
                WHERE bi.booking_id = :bId
                ORDER BY bi.booking_item_id
            """;
            Query prodQ = em.createNativeQuery(prodSql);
            prodQ.setParameter("bId", bookingId);
            List<?> prodRows = prodQ.getResultList();
            List<BookingDetailResponse.ProductSummary> products = new ArrayList<>();
            for (Object obj : prodRows) {
                Object[] r = (Object[]) obj;
                String pName = (String) r[0];
                Integer qty = ((Number) r[1]).intValue();
                BigDecimal price = new BigDecimal(r[2].toString());
                products.add(new BookingDetailResponse.ProductSummary(pName, qty, price));
            }

            return new BookingDetailResponse(
                    booking.getBookingId(),
                    showTimeSummary,
                    seats,
                    products,
                    booking.getTotalAmount(),
                    booking.getStatus()
            );
        } finally {
            em.close();
        }
    }

    // 3. PUT /bookings/{bookingId}/items: Cập nhật F&B
    public UpdateBookingItemsResponse updateBookingItems(Integer bookingId, Integer userId, List<UpdateBookingItemsRequest.Item> items) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Booking booking = em.find(Booking.class, bookingId);
            if (booking == null) {
                throw ApiException.notFound("Không tìm thấy đơn đặt vé");
            }
            if (userId != null && !booking.getUserId().equals(userId)) {
                throw ApiException.forbidden("Bạn không có quyền chỉnh sửa đơn hàng này");
            }

            LocalDateTime now = LocalDateTime.now();
            if (!"PENDING".equalsIgnoreCase(booking.getStatus())
                    || (booking.getExpiredAt() != null && booking.getExpiredAt().isBefore(now))) {
                throw ApiException.conflict("Đã hết hạn giữ ghế hoặc đơn không ở trạng thái cho phép");
            }

            // Remove existing items
            Query delQ = em.createNativeQuery("DELETE FROM cinema.booking_items WHERE booking_id = :bId");
            delQ.setParameter("bId", bookingId);
            delQ.executeUpdate();

            // Calculate seat subtotal
            Query seatSubtotalQ = em.createNativeQuery("SELECT COALESCE(SUM(price), 0) FROM cinema.show_time_seats WHERE booking_id = :bId");
            seatSubtotalQ.setParameter("bId", bookingId);
            BigDecimal seatSubtotal = new BigDecimal(seatSubtotalQ.getSingleResult().toString());

            BigDecimal newFnbTotal = BigDecimal.ZERO;
            List<UpdateBookingItemsResponse.ProductItemResponse> prodResponses = new ArrayList<>();

            if (items != null && !items.isEmpty()) {
                for (UpdateBookingItemsRequest.Item it : items) {
                    if (it.getProductId() == null || it.getQuantity() == null || it.getQuantity() <= 0) continue;

                    Query prodQ = em.createNativeQuery("SELECT product_id, product_name, price, status FROM cinema.products WHERE product_id = :pId");
                    prodQ.setParameter("pId", it.getProductId());
                    List<?> prodList = prodQ.getResultList();
                    if (prodList.isEmpty()) {
                        throw ApiException.unprocessable("Sản phẩm không tồn tại: " + it.getProductId());
                    }
                    Object[] row = (Object[]) prodList.get(0);
                    BigDecimal price = new BigDecimal(row[2].toString());
                    BigDecimal subtotal = price.multiply(BigDecimal.valueOf(it.getQuantity()));

                    BookingItem bItem = new BookingItem(booking, it.getProductId(), it.getQuantity(), price, subtotal);
                    em.persist(bItem);

                    newFnbTotal = newFnbTotal.add(subtotal);
                    prodResponses.add(new UpdateBookingItemsResponse.ProductItemResponse(it.getProductId(), it.getQuantity(), price));
                }
            }

            BigDecimal discount = booking.getDiscountAmount() != null ? booking.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal newTotal = seatSubtotal.add(newFnbTotal).subtract(discount);
            if (newTotal.compareTo(BigDecimal.ZERO) < 0) newTotal = BigDecimal.ZERO;

            booking.setTotalAmount(newTotal);
            booking.setUpdatedAt(now);
            em.merge(booking);

            em.getTransaction().commit();

            return new UpdateBookingItemsResponse(bookingId, newTotal, prodResponses);
        } catch (ApiException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ApiException.internal("Lỗi cập nhật F&B: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    // 4. PUT /bookings/{bookingId}/voucher: Áp dụng voucher
    public ApplyVoucherResponse applyVoucher(Integer bookingId, Integer userId, String code) {
        if (code == null || code.trim().isEmpty()) {
            throw ApiException.badRequest("Mã voucher không được để trống");
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Booking booking = em.find(Booking.class, bookingId);
            if (booking == null) {
                throw ApiException.notFound("Không tìm thấy đơn đặt vé");
            }
            if (userId != null && !booking.getUserId().equals(userId)) {
                throw ApiException.forbidden("Bạn không có quyền áp dụng voucher cho đơn hàng này");
            }

            LocalDateTime now = LocalDateTime.now();
            if (!"PENDING".equalsIgnoreCase(booking.getStatus())
                    || (booking.getExpiredAt() != null && booking.getExpiredAt().isBefore(now))) {
                throw ApiException.conflict("Đã hết hạn giữ ghế hoặc đơn không ở trạng thái cho phép");
            }

            // Find voucher in cinema.vouchers
            String vSql = """
                SELECT voucher_id, code, discount_type, discount_value, min_order_amount,
                       max_discount, start_date, end_date, quantity, used_count, status
                FROM cinema.vouchers
                WHERE code = :code
            """;
            Query vQuery = em.createNativeQuery(vSql);
            vQuery.setParameter("code", code.trim());
            List<?> vList = vQuery.getResultList();
            if (vList.isEmpty()) {
                throw ApiException.unprocessable("Voucher không hợp lệ hoặc đã hết lượt dùng");
            }

            Object[] vRow = (Object[]) vList.get(0);
            Integer voucherId = ((Number) vRow[0]).intValue();
            String vCode = (String) vRow[1];
            String discountType = (String) vRow[2];
            BigDecimal discountValue = new BigDecimal(vRow[3].toString());
            BigDecimal minOrderAmount = vRow[4] != null ? new BigDecimal(vRow[4].toString()) : BigDecimal.ZERO;
            BigDecimal maxDiscount = vRow[5] != null ? new BigDecimal(vRow[5].toString()) : null;
            Date startDate = (Date) vRow[6];
            Date endDate = (Date) vRow[7];
            Integer quantity = vRow[8] != null ? ((Number) vRow[8]).intValue() : 0;
            Integer usedCount = vRow[9] != null ? ((Number) vRow[9]).intValue() : 0;
            String status = (String) vRow[10];

            if (!"ACTIVE".equalsIgnoreCase(status)) {
                throw ApiException.unprocessable("Voucher không hợp lệ hoặc đã hết lượt dùng");
            }

            LocalDate today = LocalDate.now();
            if (startDate != null && today.isBefore(startDate.toLocalDate())) {
                throw ApiException.unprocessable("Voucher chưa đến thời gian áp dụng");
            }
            if (endDate != null && today.isAfter(endDate.toLocalDate())) {
                throw ApiException.unprocessable("Voucher đã hết hạn sử dụng");
            }
            if (quantity > 0 && usedCount >= quantity) {
                throw ApiException.unprocessable("Voucher không hợp lệ hoặc đã hết lượt dùng");
            }

            // Calculate original order subtotal (seat subtotal + product subtotal)
            Query seatSubtotalQ = em.createNativeQuery("SELECT COALESCE(SUM(price), 0) FROM cinema.show_time_seats WHERE booking_id = :bId");
            seatSubtotalQ.setParameter("bId", bookingId);
            BigDecimal seatSubtotal = new BigDecimal(seatSubtotalQ.getSingleResult().toString());

            Query prodSubtotalQ = em.createNativeQuery("SELECT COALESCE(SUM(subtotal), 0) FROM cinema.booking_items WHERE booking_id = :bId");
            prodSubtotalQ.setParameter("bId", bookingId);
            BigDecimal prodSubtotal = new BigDecimal(prodSubtotalQ.getSingleResult().toString());

            BigDecimal orderSubtotal = seatSubtotal.add(prodSubtotal);

            if (orderSubtotal.compareTo(minOrderAmount) < 0) {
                throw ApiException.unprocessable("Đơn hàng chưa đạt giá trị tối thiểu " + minOrderAmount.longValue() + "đ để dùng voucher");
            }

            // Calculate discount
            BigDecimal discount = BigDecimal.ZERO;
            if ("PERCENTAGE".equalsIgnoreCase(discountType)) {
                discount = orderSubtotal.multiply(discountValue).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
                if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
                    discount = maxDiscount;
                }
            } else { // FIXED_AMOUNT
                discount = discountValue;
            }

            if (discount.compareTo(orderSubtotal) > 0) {
                discount = orderSubtotal;
            }

            BigDecimal finalAmount = orderSubtotal.subtract(discount);
            if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;

            booking.setVoucherId(voucherId);
            booking.setVoucherCode(vCode);
            booking.setDiscountAmount(discount);
            booking.setTotalAmount(finalAmount);
            booking.setUpdatedAt(now);
            em.merge(booking);

            em.getTransaction().commit();

            return new ApplyVoucherResponse(bookingId, discount, finalAmount, vCode);
        } catch (ApiException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ApiException.internal("Lỗi áp dụng voucher: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    // 5. POST /bookings/{bookingId}/cancel: Hủy booking
    public CancelBookingResponse cancelBooking(Integer bookingId, Integer userId, String reason) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Booking booking = em.find(Booking.class, bookingId);
            if (booking == null) {
                throw ApiException.notFound("Không tìm thấy đơn đặt vé");
            }
            if (userId != null && !booking.getUserId().equals(userId)) {
                throw ApiException.forbidden("Bạn không có quyền hủy đơn hàng này");
            }

            if (!"PENDING".equalsIgnoreCase(booking.getStatus())) {
                throw ApiException.conflict("Chỉ hủy được đơn chưa thanh toán hoặc đang chờ thanh toán");
            }

            LocalDateTime now = LocalDateTime.now();

            // Release seats
            Query releaseQ = em.createNativeQuery("""
                UPDATE cinema.show_time_seats
                SET status = 'AVAILABLE', booking_id = NULL, hold_expiration_at = NULL
                WHERE booking_id = :bId AND status = 'SELECTED'
            """);
            releaseQ.setParameter("bId", bookingId);
            releaseQ.executeUpdate();

            // Update booking
            booking.setStatus("CANCELLED");
            booking.setCancelledAt(now);
            booking.setCancelReason(reason != null ? reason : "Người dùng yêu cầu hủy");
            booking.setUpdatedAt(now);
            em.merge(booking);

            em.getTransaction().commit();

            return new CancelBookingResponse(bookingId, "CANCELLED", now.format(ISO_FORMATTER));
        } catch (ApiException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ApiException.internal("Lỗi hủy đơn đặt vé: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    // 6. POST /bookings/{bookingId}/refund: Yêu cầu hoàn tiền
    public CreateRefundResponse createRefund(Integer bookingId, Integer userId, String reason) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Booking booking = em.find(Booking.class, bookingId);
            if (booking == null) {
                throw ApiException.notFound("Không tìm thấy đơn đặt vé");
            }
            if (userId != null && !booking.getUserId().equals(userId)) {
                throw ApiException.forbidden("Bạn không có quyền yêu cầu hoàn tiền cho đơn hàng này");
            }

            if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
                throw ApiException.unprocessable("Chỉ có thể yêu cầu hoàn tiền cho đơn hàng đã xác nhận/thanh toán thành công");
            }

            // Find completed payment
            String paySql = """
                SELECT payment_id, amount
                FROM cinema.payments
                WHERE booking_id = :bId AND status IN ('SUCCESS', 'SUCCESSFUL', 'COMPLETED')
                ORDER BY payment_id DESC
                LIMIT 1
            """;
            Query payQ = em.createNativeQuery(paySql);
            payQ.setParameter("bId", bookingId);
            List<?> payList = payQ.getResultList();
            if (payList.isEmpty()) {
                throw ApiException.unprocessable("Không tìm thấy thông tin thanh toán hợp lệ của đơn hàng");
            }

            Object[] payRow = (Object[]) payList.get(0);
            Integer paymentId = ((Number) payRow[0]).intValue();
            BigDecimal refundAmount = new BigDecimal(payRow[1].toString());

            // Check if existing refund already requested
            String checkRefSql = "SELECT refund_id FROM cinema.refunds WHERE payment_id = :pId AND status != 'FAILED'";
            Query refQ = em.createNativeQuery(checkRefSql);
            refQ.setParameter("pId", paymentId);
            if (!refQ.getResultList().isEmpty()) {
                throw ApiException.conflict("Đơn hàng đã có yêu cầu hoàn tiền đang xử lý");
            }

            // Check rule: "Không thể hoàn tiền dưới 120 phút trước suất chiếu hoặc vé đã dùng"
            String showtimeSql = """
                SELECT st.show_date, st.start_time
                FROM cinema.show_time_seats sts
                JOIN cinema.show_times st ON sts.show_time_id = st.show_time_id
                WHERE sts.booking_id = :bId
                LIMIT 1
            """;
            Query stQ = em.createNativeQuery(showtimeSql);
            stQ.setParameter("bId", bookingId);
            List<?> stList = stQ.getResultList();
            if (!stList.isEmpty()) {
                Object[] r = (Object[]) stList.get(0);
                LocalDate sDate = r[0] instanceof Date ? ((Date) r[0]).toLocalDate() : (LocalDate) r[0];
                LocalTime sTime = r[1] instanceof Time ? ((Time) r[1]).toLocalTime() : (LocalTime) r[1];
                LocalDateTime showTimeDt = LocalDateTime.of(sDate, sTime);
                if (LocalDateTime.now().plusMinutes(120).isAfter(showTimeDt)) {
                    throw ApiException.unprocessable("Không thể hoàn tiền dưới 120 phút trước suất chiếu hoặc vé đã dùng");
                }
            }

            // Check if any ticket is used
            Query tQ = em.createNativeQuery("SELECT COUNT(*) FROM cinema.tickets WHERE booking_id = :bId AND status = 'USED'");
            tQ.setParameter("bId", bookingId);
            long usedCount = ((Number) tQ.getSingleResult()).longValue();
            if (usedCount > 0) {
                throw ApiException.unprocessable("Không thể hoàn tiền dưới 120 phút trước suất chiếu hoặc vé đã dùng");
            }

            LocalDateTime now = LocalDateTime.now();
            Refund refund = new Refund(paymentId, refundAmount, reason != null ? reason : "Yêu cầu hoàn tiền", "PROCESSING");
            refund.setRefundDate(now);
            em.persist(refund);
            em.flush();

            em.getTransaction().commit();

            return new CreateRefundResponse(
                    refund.getRefundId(),
                    bookingId,
                    refundAmount,
                    "PROCESSING",
                    now.format(ISO_FORMATTER)
            );
        } catch (ApiException e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw ApiException.internal("Lỗi tạo yêu cầu hoàn tiền: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    // 7. GET /bookings/{bookingId}/refund: Xem tiến trình refund
    public RefundDetailResponse getRefundDetail(Integer bookingId, Integer userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Booking booking = em.find(Booking.class, bookingId);
            if (booking == null) {
                throw ApiException.notFound("Không tìm thấy đơn đặt vé");
            }
            if (userId != null && !booking.getUserId().equals(userId)) {
                throw ApiException.forbidden("Bạn không có quyền xem thông tin hoàn tiền của đơn hàng này");
            }

            String refSql = """
                SELECT r.refund_id, r.amount, r.status, r.refund_date
                FROM cinema.refunds r
                JOIN cinema.payments p ON r.payment_id = p.payment_id
                WHERE p.booking_id = :bId
                ORDER BY r.refund_id DESC
                LIMIT 1
            """;
            Query refQ = em.createNativeQuery(refSql);
            refQ.setParameter("bId", bookingId);
            List<?> refList = refQ.getResultList();
            if (refList.isEmpty()) {
                throw ApiException.notFound("Đơn hàng chưa có yêu cầu hoàn tiền");
            }

            Object[] row = (Object[]) refList.get(0);
            Integer refId = ((Number) row[0]).intValue();
            BigDecimal amount = new BigDecimal(row[1].toString());
            String status = (String) row[2];
            Timestamp refDate = (Timestamp) row[3];
            String refundedAt = refDate != null ? refDate.toLocalDateTime().format(ISO_FORMATTER) : null;

            return new RefundDetailResponse(refId, bookingId, amount, status, refundedAt);
        } finally {
            em.close();
        }
    }
}
