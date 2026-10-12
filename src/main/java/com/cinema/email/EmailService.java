package com.cinema.email;

import com.cinema.common.config.ObjectMapperConfig;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dịch vụ gửi email qua Google Apps Script Webhook (HTTPS Port 443):
 * - Không bị Render chặn cổng SMTP
 * - Gửi trực tiếp từ tài khoản Gmail của bạn
 * - Cấu hình duy nhất qua biến MAIL_WEBHOOK_URL trong .env
 */
public class EmailService {

    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    // Client HTTP theo dõi chuyển hướng (Redirect 302 của Google Script)
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static String getWebhookUrl() {
        return getEnv("MAIL_WEBHOOK_URL", "EMAIL_WEBHOOK_URL");
    }

    public static boolean isConfigured() {
        String url = getWebhookUrl();
        return url != null && !url.isBlank();
    }

    /**
     * Gửi email HTML qua Webhook Google Script
     */
    public static void sendHtmlMail(String to, String subject, String htmlContent) throws Exception {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ email người nhận không được để trống.");
        }
        if (!isConfigured()) {
            LOGGER.warning("Chưa cấu hình MAIL_WEBHOOK_URL trong file .env. Bỏ qua việc gửi email.");
            return;
        }

        Map<String, String> payload = Map.of(
                "to", to.trim(),
                "subject", subject != null ? subject : "Thông báo từ Cinema Booking",
                "html", htmlContent != null ? htmlContent : ""
        );

        String json = ObjectMapperConfig.getInstance().writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(getWebhookUrl().trim()))
                .header("Content-Type", "application/json; charset=UTF-8")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new RuntimeException("Google Webhook trả về lỗi HTTP " + response.statusCode() + ": " + response.body());
        }
    }

    /**
     * Gửi email HTML bất đồng bộ (chạy nền, không làm gián đoạn luồng đặt vé chính)
     */
    public static CompletableFuture<Void> sendHtmlMailAsync(String to, String subject, String htmlContent) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMail(to, subject, htmlContent);
                LOGGER.info("Gửi email thành công tới: " + to + " (Tiêu đề: " + subject + ")");
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Gửi email thất bại tới: " + to + " - Lỗi: " + e.getMessage());
            }
        });
    }

    private static String getEnv(String... keys) {
        for (String key : keys) {
            String val = null;
            try {
                val = dotenv.get(key);
            } catch (Exception ignored) {}
            if (val == null || val.isBlank()) {
                val = System.getenv(key);
            }
            if (val == null || val.isBlank()) {
                val = System.getProperty(key);
            }
            if (val != null && !val.isBlank()) {
                return val.trim();
            }
        }
        return null;
    }
}
