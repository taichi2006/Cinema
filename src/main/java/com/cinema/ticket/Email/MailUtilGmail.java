package com.cinema.ticket.Email;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tiện ích gửi email qua giao thức JavaMail SMTP
 */
public class MailUtilGmail {

    private static final Logger LOGGER = Logger.getLogger(MailUtilGmail.class.getName());

    /**
     * Gửi email cơ bản qua Google SMTP
     *
     * @param to         Địa chỉ email người nhận
     * @param from       Địa chỉ email người gửi (nếu null lấy mặc định từ
     *                   MailConfig)
     * @param subject    Tiêu đề thư
     * @param body       Nội dung thư
     * @param bodyIsHTML true nếu là HTML, false nếu là văn bản thuần
     * @throws MessagingException khi xảy ra lỗi kết nối SMTP hoặc xác thực
     */
    public static void sendMail(String to, String from,
            String subject, String body, boolean bodyIsHTML)
            throws MessagingException {

        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ email người nhận 'to' không được để trống.");
        }

        Session session = MailConfig.createSession();
        MimeMessage message = new MimeMessage(session);

        String fromEmail = (from != null && !from.isBlank()) ? from.trim() : MailConfig.getUsername();
        if (fromEmail == null || fromEmail.isBlank()) {
            throw new IllegalStateException(
                    "Chưa cấu hình tài khoản gửi mail trong biến môi trường MAIL_USERNAME hoặc .env");
        }

        try {
            String fromName = MailConfig.getFromName();
            message.setFrom(new InternetAddress(fromEmail, fromName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            message.setFrom(new InternetAddress(fromEmail));
        }

        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to.trim()));
        message.setSubject(subject != null ? subject : "Thông báo vé xem phim - Cinema Booking", "UTF-8");

        if (bodyIsHTML) {
            message.setContent(body, "text/html; charset=UTF-8");
        } else {
            message.setText(body, "UTF-8");
        }

        Transport.send(message);
    }

    /**
     * Gửi mail nội dung HTML với người gửi mặc định từ MailConfig
     */
    public static void sendHtmlMail(String to, String subject, String htmlContent) throws MessagingException {
        sendMail(to, MailConfig.getUsername(), subject, htmlContent, true);
    }

    /**
     * Gửi mail HTML bất đồng bộ (Background Async):
     * Không làm nghẽn luồng xử lý chính và không làm gián đoạn giao dịch nếu kết
     * nối mạng chậm.
     */
    public static CompletableFuture<Void> sendHtmlMailAsync(String to, String subject, String htmlContent) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendHtmlMail(to, subject, htmlContent);
                LOGGER.info("Đã gửi email thành công tới: " + to + " (Tiêu đề: " + subject + ")");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Gửi email thất bại tới: " + to + " - Lỗi: " + e.getMessage(), e);
            }
        });
    }
}
