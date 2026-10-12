package com.cinema.email;

import com.cinema.common.util.JPAUtil;
import com.cinema.ticket.Ticket;
import com.cinema.ticket.TicketDAO;
import com.cinema.user.entity.User;
import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service gửi email vé xem phim cho khách hàng khi đặt vé/thanh toán thành công.
 * Toàn bộ giao diện HTML và CSS được quản lý độc lập trong file mail.jsp (không nhúng HTML/CSS trong code Java).
 */
public class TicketEmailService {

    private static final Logger LOGGER = Logger.getLogger(TicketEmailService.class.getName());
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final TicketDAO ticketDAO = new TicketDAO();

    /**
     * Gửi email vé tự động theo mã đơn đặt vé (bookingId) và mã người dùng (userId)
     */
    public CompletableFuture<Void> sendTicketEmailByBookingAsync(Long bookingId, Long userId) {
        if (bookingId == null || userId == null) {
            return CompletableFuture.completedFuture(null);
        }

        if (!EmailService.isConfigured()) {
            LOGGER.info("Bỏ qua gửi email: chưa cấu hình MAIL_WEBHOOK_URL trong .env");
            return CompletableFuture.completedFuture(null);
        }

        EntityManager em = JPAUtil.getEntityManager();
        try {
            User user = em.find(User.class, userId);
            if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
                LOGGER.warning("Không tìm thấy email của người dùng #" + userId);
                return CompletableFuture.completedFuture(null);
            }

            List<Ticket> tickets = ticketDAO.findByBookingId(bookingId);
            if (tickets.isEmpty()) {
                LOGGER.warning("Đơn hàng #" + bookingId + " chưa có vé nào.");
                return CompletableFuture.completedFuture(null);
            }

            String movieTitle = tickets.get(0).getMovieName() != null ? tickets.get(0).getMovieName() : "Vé Xem Phim";
            String subject = "[Cinema Booking] Xác nhận đặt vé thành công: " + movieTitle;

            // Tải giao diện HTML đã render từ file mail.jsp (hoàn toàn tách biệt HTML/CSS khỏi Java code)
            String html = fetchHtmlFromMailJsp(bookingId, user.getFullName());

            return EmailService.sendHtmlMailAsync(user.getEmail(), subject, html);
        } catch (Exception e) {
            LOGGER.warning("Lỗi chuẩn bị dữ liệu gửi email cho đơn #" + bookingId + ": " + e.getMessage());
            return CompletableFuture.completedFuture(null);
        } finally {
            em.close();
        }
    }

    /**
     * Tải nội dung HTML từ file mail.jsp theo bookingId
     */
    private String fetchHtmlFromMailJsp(Long bookingId, String customerName) {
        String baseUrl = getBaseUrl();
        String url = baseUrl + "/mail.jsp?bookingId=" + bookingId;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                return response.body();
            }
            LOGGER.warning("Không thể tải mail.jsp từ " + url + " (Mã HTTP: " + response.statusCode() + ")");
        } catch (Exception e) {
            LOGGER.warning("Lỗi kết nối tải mail.jsp (" + url + "): " + e.getMessage());
        }

        // Fallback văn bản thuần ngắn gọn nếu không kết nối được tới mail.jsp
        return "Xin chào " + (customerName != null ? customerName : "Quý khách")
                + ", đơn đặt vé #" + bookingId + " của bạn đã thanh toán thành công. Vui lòng kiểm tra mã vé trên website.";
    }

    private static String getBaseUrl() {
        String appUrl = System.getenv("APP_URL");
        if (appUrl == null || appUrl.isBlank()) {
            appUrl = dotenv.get("APP_URL");
        }
        if (appUrl == null || appUrl.isBlank()) {
            String vnpReturn = dotenv.get("VNPAY_RETURN_URL");
            if (vnpReturn != null && vnpReturn.contains("/vn_return.jsp")) {
                appUrl = vnpReturn.substring(0, vnpReturn.indexOf("/vn_return.jsp"));
            }
        }
        if (appUrl == null || appUrl.isBlank()) {
            String port = System.getenv("PORT");
            if (port != null && !port.isBlank()) {
                appUrl = "http://localhost:" + port.trim();
            }
        }
        return (appUrl != null && !appUrl.isBlank()) ? appUrl.replaceAll("/$", "") : "http://localhost:8080";
    }
}
