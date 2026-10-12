package com.cinema.ticket.Email;

import com.cinema.booking.entity.Booking;
import com.cinema.common.util.JPAUtil;
import com.cinema.ticket.Ticket;
import com.cinema.ticket.TicketDAO;
import com.cinema.user.entity.User;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service nghiệp vụ điều phối gửi email xác nhận vé xem phim điện tử cho người dùng.
 */
public class TicketEmailService {

    private static final Logger LOGGER = Logger.getLogger(TicketEmailService.class.getName());

    private final TicketDAO ticketDAO;

    public TicketEmailService() {
        this.ticketDAO = new TicketDAO();
    }

    public TicketEmailService(TicketDAO ticketDAO) {
        this.ticketDAO = ticketDAO;
    }

    /**
     * Gửi email xác nhận vé đầy đủ tham số (Chạy bất đồng bộ)
     */
    public CompletableFuture<Void> sendTicketEmailAsync(
            String recipientEmail,
            String recipientName,
            String movieTitle,
            String cinemaName,
            String roomName,
            LocalDateTime showTime,
            List<Ticket> tickets,
            BigDecimal totalAmount) {

        if (recipientEmail == null || recipientEmail.isBlank()) {
            LOGGER.warning("Không thể gửi email vé: địa chỉ email người nhận trống.");
            return CompletableFuture.completedFuture(null);
        }

        if (!MailConfig.isConfigured()) {
            LOGGER.warning("Chưa cấu hình thông tin gửi mail (MAIL_USERNAME / MAIL_PASSWORD) trong file .env hoặc biến môi trường. Bỏ qua việc gửi mail.");
            return CompletableFuture.completedFuture(null);
        }

        String subject = "[Cinema Booking] Xác nhận vé xem phim: " + (movieTitle != null ? movieTitle : "");
        String htmlContent = TicketEmailTemplate.buildTicketEmailHtml(
                recipientName, movieTitle, cinemaName, roomName, showTime, tickets, totalAmount
        );

        return MailUtilGmail.sendHtmlMailAsync(recipientEmail, subject, htmlContent);
    }

    /**
     * Gửi email vé tự động theo mã đơn đặt vé (bookingId) và mã người dùng (userId)
     */
    public CompletableFuture<Void> sendTicketEmailByBookingAsync(Long bookingId, Long userId) {
        if (bookingId == null || userId == null) {
            return CompletableFuture.completedFuture(null);
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, userId);
            Booking booking = em.find(Booking.class, bookingId);

            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                LOGGER.warning("Không tìm thấy thông tin email của người dùng #" + userId);
                return CompletableFuture.completedFuture(null);
            }

            List<Ticket> tickets = ticketDAO.findByBookingId(bookingId);
            if (tickets.isEmpty()) {
                LOGGER.warning("Đơn hàng #" + bookingId + " chưa có vé nào được phát hành.");
                return CompletableFuture.completedFuture(null);
            }

            // Trích xuất thông tin chung từ vé đầu tiên
            Ticket firstTicket = tickets.get(0);
            String movieTitle = firstTicket.getMovieName();
            String roomName = firstTicket.getRoomName();
            LocalDateTime showTime = firstTicket.getShowTimeStartTime();
            String cinemaName = "Rạp chiếu phim Cinema";
            try {
                var cinemaQuery = em.createNativeQuery("""
                        SELECT c.cinema_name 
                        FROM cinema.rooms r 
                        JOIN cinema.cinemas c ON c.cinema_id = r.cinema_id 
                        WHERE r.room_name = :roomName 
                        LIMIT 1
                        """);
                cinemaQuery.setParameter("roomName", roomName);
                List<?> cinemaResults = cinemaQuery.getResultList();
                if (!cinemaResults.isEmpty() && cinemaResults.get(0) != null) {
                    cinemaName = cinemaResults.get(0).toString();
                }
            } catch (Exception ignored) {
            }

            BigDecimal totalAmount = (booking != null && booking.getTotalAmount() != null)
                    ? booking.getTotalAmount()
                    : tickets.stream().map(Ticket::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

            return sendTicketEmailAsync(
                    user.getEmail(),
                    user.getFullName(),
                    movieTitle,
                    cinemaName,
                    roomName,
                    showTime,
                    tickets,
                    totalAmount
            );
        } catch (Exception e) {
            LOGGER.warning("Lỗi khi chuẩn bị dữ liệu gửi email cho booking #" + bookingId + ": " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        } finally {
            em.close();
        }
    }
}
