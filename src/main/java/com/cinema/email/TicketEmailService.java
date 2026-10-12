package com.cinema.email;

import com.cinema.booking.entity.Booking;
import com.cinema.common.util.JPAUtil;
import com.cinema.ticket.Ticket;
import com.cinema.ticket.TicketDAO;
import com.cinema.user.entity.User;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service gửi email vé xem phim cho khách hàng khi đặt vé/thanh toán thành công.
 */
public class TicketEmailService {

    private static final Logger LOGGER = Logger.getLogger(TicketEmailService.class.getName());
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(Locale.of("vi", "VN"));
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    private final TicketDAO ticketDAO = new TicketDAO();

    /**
     * Gửi email vé tự động theo mã đơn đặt vé (bookingId) và mã người dùng (userId)
     */
    public CompletableFuture<Void> sendTicketEmailByBookingAsync(Long bookingId, Long userId) {
        if (bookingId == null || userId == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (!EmailService.isConfigured()) {
            LOGGER.info("Bỏ qua gửi email: chưa cấu hình MAIL_USERNAME / MAIL_PASSWORD trong .env");
            return CompletableFuture.completedFuture(null);
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, userId);
            Booking booking = em.find(Booking.class, bookingId);

            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                LOGGER.warning("Không tìm thấy email của người dùng #" + userId);
                return CompletableFuture.completedFuture(null);
            }

            List<Ticket> tickets = ticketDAO.findByBookingId(bookingId);
            if (tickets.isEmpty()) {
                LOGGER.warning("Đơn hàng #" + bookingId + " chưa có vé nào.");
                return CompletableFuture.completedFuture(null);
            }

            Ticket firstTicket = tickets.get(0);
            String movieTitle = firstTicket.getMovieName() != null ? firstTicket.getMovieName() : "Vé Xem Phim";
            String roomName = firstTicket.getRoomName() != null ? firstTicket.getRoomName() : "Phòng Chiếu";
            LocalDateTime showTime = firstTicket.getShowTimeStartTime();
            String formattedTime = showTime != null ? showTime.format(DATE_FORMATTER) : "Theo thông tin trên vé";

            String cinemaName = "Rạp Cinema Star";
            try {
                var query = em.createNativeQuery("""
                        SELECT c.cinema_name 
                        FROM cinema.rooms r 
                        JOIN cinema.cinemas c ON c.cinema_id = r.cinema_id 
                        WHERE r.room_name = :roomName 
                        LIMIT 1
                        """);
                query.setParameter("roomName", roomName);
                List<?> list = query.getResultList();
                if (!list.isEmpty() && list.get(0) != null) {
                    cinemaName = list.get(0).toString();
                }
            } catch (Exception ignored) {}

            BigDecimal total = (booking != null && booking.getTotalAmount() != null)
                    ? booking.getTotalAmount()
                    : tickets.stream().map(Ticket::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);

            String html = buildEmailHtml(user.getFullName(), movieTitle, cinemaName, roomName, formattedTime, tickets, total);
            String subject = "[Cinema Star] Xác nhận đặt vé thành công: " + movieTitle;

            return EmailService.sendHtmlMailAsync(user.getEmail(), subject, html);
        } catch (Exception e) {
            LOGGER.warning("Lỗi chuẩn bị dữ liệu gửi email cho đơn #" + bookingId + ": " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        } finally {
            em.close();
        }
    }

    private String buildEmailHtml(String name, String movie, String cinema, String room,
                                  String time, List<Ticket> tickets, BigDecimal total) {
        StringBuilder rows = new StringBuilder();
        int stt = 1;
        for (Ticket t : tickets) {
            String seat = t.getSeatName() != null ? t.getSeatName() : "-";
            String code = t.getTicketCode() != null ? t.getTicketCode() : "TKT-" + t.getTicketId();
            String price = t.getPrice() != null ? CURRENCY_FORMAT.format(t.getPrice()) + " ₫" : "-";
            rows.append(String.format(
                    "<tr><td style='padding:8px;border:1px solid #ddd;text-align:center;'>%d</td>" +
                    "<td style='padding:8px;border:1px solid #ddd;text-align:center;'><b>%s</b></td>" +
                    "<td style='padding:8px;border:1px solid #ddd;text-align:center;font-family:monospace;'>%s</td>" +
                    "<td style='padding:8px;border:1px solid #ddd;text-align:right;'>%s</td></tr>",
                    stt++, seat, code, price));
        }

        String totalStr = total != null ? CURRENCY_FORMAT.format(total) + " ₫" : "0 ₫";
        String customerName = (name != null && !name.isBlank()) ? name : "Quý khách";

        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset='UTF-8'></head>
                <body style='font-family: Arial, sans-serif; background-color: #f7f7f7; padding: 20px; color: #333;'>
                    <div style='max-width: 600px; margin: auto; background: #fff; padding: 25px; border-radius: 8px; border: 1px solid #e0e0e0;'>
                        <h2 style='color: #e50914; margin-top: 0;'>XÁC NHẬN ĐẶT VÉ THÀNH CÔNG</h2>
                        <p>Xin chào <b>%s</b>,</p>
                        <p>Bạn đã thanh toán thành công đơn đặt vé tại hệ thống rạp <b>%s</b>.</p>
                        <div style='background: #f9f9f9; padding: 15px; border-radius: 6px; margin: 15px 0;'>
                            <p style='margin: 4px 0;'>🎬 <b>Phim:</b> %s</p>
                            <p style='margin: 4px 0;'>🏢 <b>Rạp:</b> %s</p>
                            <p style='margin: 4px 0;'>🚪 <b>Phòng:</b> %s</p>
                            <p style='margin: 4px 0;'>⏰ <b>Suất chiếu:</b> %s</p>
                        </div>
                        <table style='width: 100%%; border-collapse: collapse; margin-top: 15px;'>
                            <thead>
                                <tr style='background: #eee;'>
                                    <th style='padding:8px;border:1px solid #ddd;'>STT</th>
                                    <th style='padding:8px;border:1px solid #ddd;'>Ghế</th>
                                    <th style='padding:8px;border:1px solid #ddd;'>Mã vé</th>
                                    <th style='padding:8px;border:1px solid #ddd;'>Giá</th>
                                </tr>
                            </thead>
                            <tbody>
                                %s
                            </tbody>
                            <tfoot>
                                <tr>
                                    <td colspan='3' style='padding:10px;text-align:right;font-weight:bold;border:1px solid #ddd;'>Tổng thanh toán:</td>
                                    <td style='padding:10px;text-align:right;font-weight:bold;color:#e50914;border:1px solid #ddd;'>%s</td>
                                </tr>
                            </tfoot>
                        </table>
                        <p style='margin-top: 20px; font-size: 13px; color: #666;'>
                            * Vui lòng xuất trình mã vé tại quầy trước giờ chiếu 15 phút. Chúc bạn xem phim vui vẻ!
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(customerName, cinema, movie, cinema, room, time, rows.toString(), totalStr);
    }
}
