package com.cinema.booking.dao;

import com.cinema.booking.dto.response.CreateRefundResponse;
import com.cinema.booking.dto.response.RefundDetailResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Refund;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.showtime.entity.Showtime;
import com.cinema.ticket.enums.TicketStatus;
import com.cinema.user.entity.User;
import com.cinema.wallet.entity.Wallet;
import com.cinema.wallet.entity.WalletTransaction;
import com.cinema.wallet.enums.TransactionStatus;
import com.cinema.wallet.enums.TransactionType;
import com.cinema.wallet.enums.WalletStatus;
import com.cinema.websocket.SeatSessionManager;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RefundDAO {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public CreateRefundResponse createRefund(Long bookingId, Long userId, String reason) {
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
            if ("REFUNDED".equalsIgnoreCase(booking.getStatus())) {
                throw ApiException.conflict("Đơn đặt vé này đã được hoàn tiền trước đó");
            }
            if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus()) && !"PAID".equalsIgnoreCase(booking.getStatus())) {
                throw ApiException.unprocessable("Chỉ có thể hoàn tiền cho đơn hàng đã thanh toán thành công");
            }

            List<?> payList = em.createNativeQuery(
                    "SELECT payment_id, amount FROM cinema.payments " +
                            "WHERE booking_id = :bId AND status IN ('SUCCESS', 'SUCCESSFUL', 'COMPLETED') " +
                            "ORDER BY payment_id DESC LIMIT 1")
                    .setParameter("bId", bookingId).getResultList();

            if (payList.isEmpty()) {
                throw ApiException.unprocessable("Không tìm thấy thông tin thanh toán hợp lệ của đơn hàng");
            }

            Object[] payRow = (Object[]) payList.get(0);
            Long paymentId = ((Number) payRow[0]).longValue();
            BigDecimal refundAmount = new BigDecimal(payRow[1].toString());

            List<Refund> existingRefunds = em.createQuery(
                    "SELECT r FROM Refund r WHERE r.paymentId = :pId AND r.status != 'FAILED'",
                    Refund.class).setParameter("pId", paymentId).getResultList();

            if (!existingRefunds.isEmpty()) {
                throw ApiException.conflict("Đơn hàng đã được hoàn tiền hoặc đang xử lý");
            }

            // 1. Kiểm tra thời gian suất chiếu: Chỉ hoàn tiền trước giờ chiếu ít nhất 24
            // giờ
            java.time.ZoneId vnZone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
            LocalDateTime now = LocalDateTime.now(vnZone);
            LocalDateTime showTimeDt = null;
            List<Showtime> showtimes = em.createQuery(
                    "SELECT st FROM ShowtimeSeat sts, Showtime st " +
                            "WHERE sts.showTimeId = st.showTimeId AND sts.bookingId = :bId",
                    Showtime.class).setParameter("bId", bookingId).setMaxResults(1).getResultList();

            if (!showtimes.isEmpty()) {
                Showtime st = showtimes.get(0);
                showTimeDt = LocalDateTime.of(st.getShowDate(), st.getStartTime());
            } else {
                List<LocalDateTime> ticketShowtimes = em.createQuery(
                        "SELECT t.showTimeStartTime FROM Ticket t WHERE t.bookingId = :bId AND t.showTimeStartTime IS NOT NULL",
                        LocalDateTime.class).setParameter("bId", bookingId).setMaxResults(1).getResultList();
                if (!ticketShowtimes.isEmpty()) {
                    showTimeDt = ticketShowtimes.get(0);
                }
            }

            if (showTimeDt != null) {
                if (now.plusHours(24).isAfter(showTimeDt)) {
                    String timeStr = showTimeDt.format(DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy"));
                    throw ApiException.unprocessable(
                            "Chỉ có thể yêu cầu hoàn tiền trước giờ chiếu ít nhất 24 giờ. Suất chiếu của bạn: "
                                    + timeStr);
                }
            }

            // 2. Kiểm tra xem vé đã được check-in / sử dụng chưa
            Long usedTickets = em.createQuery(
                    "SELECT COUNT(t) FROM Ticket t WHERE t.bookingId = :bId AND t.status = :status",
                    Long.class).setParameter("bId", bookingId).setParameter("status", TicketStatus.USED)
                    .getSingleResult();

            if (usedTickets != null && usedTickets > 0) {
                throw ApiException.unprocessable("Vé đã được sử dụng, không thể hoàn tiền");
            }

            // 3. Hoàn tiền trực tiếp vào Ví (Wallet) của người dùng
            Wallet wallet = em.createQuery(
                    "SELECT w FROM Wallet w WHERE w.user.id = :uId", Wallet.class)
                    .setParameter("uId", userId)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);

            if (wallet == null) {
                User user = em.find(User.class, userId);
                wallet = new Wallet(user);
                wallet.setBalance(BigDecimal.ZERO);
                wallet.setStatus(WalletStatus.ACTIVE);
                em.persist(wallet);
                em.flush();
            }

            if (wallet.getStatus() != WalletStatus.ACTIVE) {
                throw ApiException.forbidden("Ví của bạn đang bị khóa, không thể nhận tiền hoàn");
            }

            wallet.setBalance(wallet.getBalance().add(refundAmount));

            WalletTransaction walletTransaction = new WalletTransaction();
            walletTransaction.setWallet(wallet);
            walletTransaction.setAmount(refundAmount);
            walletTransaction.setType(TransactionType.REFUND);
            walletTransaction.setStatus(TransactionStatus.SUCCESSFUL);
            walletTransaction.setDescription("Hoàn tiền đơn đặt vé #" + bookingId
                    + (reason != null && !reason.isBlank() ? " (" + reason.trim() + ")" : ""));
            em.persist(walletTransaction);
            em.flush();

            // 4. Hủy vé của đơn hàng (chuyển sang REFUNDED)
            em.createQuery("UPDATE Ticket t SET t.status = :status WHERE t.bookingId = :bId")
                    .setParameter("status", TicketStatus.REFUNDED)
                    .setParameter("bId", bookingId)
                    .executeUpdate();

            // 5. Tìm tất cả các ghế thuộc về đơn hàng này để giải phóng
            List<Object[]> refundedSeats = em.createQuery(
                    "SELECT DISTINCT s.showTimeId, s.seatId FROM ShowtimeSeat s " +
                    "WHERE s.bookingId = :bId OR s.id IN (SELECT t.showTimeSeatId FROM Ticket t WHERE t.bookingId = :bId)",
                    Object[].class)
                    .setParameter("bId", bookingId)
                    .getResultList();

            // Nhả lại ghế trống (AVAILABLE) trong CSDL
            em.createQuery(
                    "UPDATE ShowtimeSeat s SET s.status = 'AVAILABLE', s.bookingId = null, s.holdExpirationAt = null " +
                    "WHERE s.bookingId = :bId OR s.id IN (SELECT t.showTimeSeatId FROM Ticket t WHERE t.bookingId = :bId)")
                    .setParameter("bId", bookingId)
                    .executeUpdate();

            // 6. Khôi phục voucher đã dùng (nếu có)
            if (booking.getVoucherId() != null) {
                em.createNativeQuery(
                        "UPDATE cinema.user_vouchers SET status = 'UNUSED', used_at = NULL " +
                                "WHERE user_id = :uId AND voucher_id = :vId AND status = 'USED'")
                        .setParameter("uId", userId).setParameter("vId", booking.getVoucherId()).executeUpdate();
            }

            // 7. Cập nhật trạng thái đơn hàng thành REFUNDED
            booking.setStatus("REFUNDED");
            booking.setUpdatedAt(now);

            // 8. Tạo bản ghi hoàn tiền với trạng thái COMPLETED
            Refund refund = new Refund(paymentId, refundAmount, reason != null ? reason : "Hoàn tiền vé vào ví",
                    "COMPLETED");
            refund.setRefundDate(now);
            refund.setGatewayRefundId("WALLET_TX_" + walletTransaction.getId());
            em.persist(refund);
            em.flush();

            em.getTransaction().commit();

            // 9. Đồng bộ nhả ghế thời gian thực qua WebSocket và xóa khỏi bộ nhớ RAM
            try {
                for (Object[] r : refundedSeats) {
                    if (r != null && r[0] != null && r[1] != null) {
                        Long stId = ((Number) r[0]).longValue();
                        Long sId = ((Number) r[1]).longValue();
                        SeatSessionManager.getInstance().releaseBookedSeat(stId, sId);
                    }
                }
            } catch (Exception ignored) {}

            return new CreateRefundResponse(
                    refund.getRefundId(),
                    bookingId,
                    refundAmount,
                    "COMPLETED",
                    now.format(ISO_FORMATTER));
        } catch (ApiException e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            throw e;
        } catch (Exception e) {
            if (em.getTransaction().isActive())
                em.getTransaction().rollback();
            throw ApiException.internal("Lỗi hoàn tiền vé: " + e.getMessage());
        } finally {
            em.close();
        }
    }

    public RefundDetailResponse getRefundDetail(Long bookingId, Long userId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Booking booking = em.find(Booking.class, bookingId);
            if (booking == null) {
                throw ApiException.notFound("Không tìm thấy đơn đặt vé");
            }
            if (userId != null && !booking.getUserId().equals(userId)) {
                throw ApiException.forbidden("Bạn không có quyền xem thông tin hoàn tiền của đơn hàng này");
            }

            List<?> refList = em.createNativeQuery(
                    "SELECT r.refund_id, r.amount, r.status, r.refund_date " +
                            "FROM cinema.refunds r JOIN cinema.payments p ON r.payment_id = p.payment_id " +
                            "WHERE p.booking_id = :bId ORDER BY r.refund_id DESC LIMIT 1")
                    .setParameter("bId", bookingId).getResultList();

            if (refList.isEmpty()) {
                throw ApiException.notFound("Đơn hàng chưa có yêu cầu hoàn tiền");
            }

            Object[] row = (Object[]) refList.get(0);
            Long refId = ((Number) row[0]).longValue();
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
