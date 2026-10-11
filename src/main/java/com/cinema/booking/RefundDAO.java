package com.cinema.booking;

import com.cinema.booking.dto.CreateRefundResponse;
import com.cinema.booking.dto.RefundDetailResponse;
import com.cinema.booking.entity.Booking;
import com.cinema.booking.entity.Refund;
import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.showtime.Showtime;
import com.cinema.ticket.TicketStatus;
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
            if (!"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
                throw ApiException.unprocessable("Chỉ có thể yêu cầu hoàn tiền cho đơn hàng đã xác nhận/thanh toán thành công");
            }

            List<?> payList = em.createNativeQuery(
                    "SELECT payment_id, amount FROM cinema.payments " +
                    "WHERE booking_id = :bId AND status IN ('SUCCESS', 'SUCCESSFUL', 'COMPLETED') " +
                    "ORDER BY payment_id DESC LIMIT 1"
            ).setParameter("bId", bookingId).getResultList();

            if (payList.isEmpty()) {
                throw ApiException.unprocessable("Không tìm thấy thông tin thanh toán hợp lệ của đơn hàng");
            }

            Object[] payRow = (Object[]) payList.get(0);
            Long paymentId = ((Number) payRow[0]).longValue();
            BigDecimal refundAmount = new BigDecimal(payRow[1].toString());

            List<Refund> existingRefunds = em.createQuery(
                    "SELECT r FROM Refund r WHERE r.paymentId = :pId AND r.status != 'FAILED'",
                    Refund.class
            ).setParameter("pId", paymentId).getResultList();

            if (!existingRefunds.isEmpty()) {
                throw ApiException.conflict("Đơn hàng đã có yêu cầu hoàn tiền đang xử lý");
            }

            List<Showtime> showtimes = em.createQuery(
                    "SELECT st FROM ShowtimeSeat sts, Showtime st " +
                    "WHERE sts.showTimeId = st.showTimeId AND sts.bookingId = :bId",
                    Showtime.class
            ).setParameter("bId", bookingId).setMaxResults(1).getResultList();

            if (!showtimes.isEmpty()) {
                Showtime st = showtimes.get(0);
                LocalDateTime showTimeDt = LocalDateTime.of(st.getShowDate(), st.getStartTime());
                if (LocalDateTime.now().plusMinutes(120).isAfter(showTimeDt)) {
                    throw ApiException.unprocessable("Không thể hoàn tiền dưới 120 phút trước suất chiếu hoặc vé đã dùng");
                }
            }

            Long usedTickets = em.createQuery(
                    "SELECT COUNT(t) FROM Ticket t WHERE t.bookingId = :bId AND t.status = :status",
                    Long.class
            ).setParameter("bId", bookingId).setParameter("status", TicketStatus.USED).getSingleResult();

            if (usedTickets != null && usedTickets > 0) {
                throw ApiException.unprocessable("Không thể hoàn tiền dưới 120 phút trước suất chiếu hoặc vé đã dùng");
            }

            LocalDateTime now = LocalDateTime.now();
            Refund refund = new Refund(paymentId, refundAmount, reason != null ? reason : "Yêu cầu hoàn tiền", "PENDING");
            refund.setRefundDate(now);
            em.persist(refund);
            em.flush();

            em.getTransaction().commit();

            return new CreateRefundResponse(
                    refund.getRefundId(),
                    bookingId,
                    refundAmount,
                    "PENDING",
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
                    "WHERE p.booking_id = :bId ORDER BY r.refund_id DESC LIMIT 1"
            ).setParameter("bId", bookingId).getResultList();

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
