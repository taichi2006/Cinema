package com.cinema.ticket.Email;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;

import java.io.InputStream;
import java.util.Properties;

/**
 * Cấu hình JavaMail SMTP cho dịch vụ gửi mail thông báo vé xem phim:
 * - Hỗ trợ nạp cấu hình từ .env hoặc biến môi trường hệ thống.
 * - Tương thích hoàn toàn với Google App Password (mật khẩu ứng dụng 16 ký tự).
 */
public class MailConfig {

    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private static final String HOST = getEnvOrDefault("MAIL_HOST", "smtp.gmail.com");
    private static final String PORT = getEnvOrDefault("MAIL_PORT", "587");
    private static final String USERNAME = getEnv("MAIL_USERNAME");
    private static final String PASSWORD = getEnv("MAIL_PASSWORD");
    private static final String FROM_NAME = getEnvOrDefault("MAIL_FROM_NAME", "Cinema Star Booking");

    private static volatile String customUsername = null;
    private static volatile String customPassword = null;

    public static void setCredentials(String username, String password) {
        customUsername = username != null ? username.trim() : null;
        customPassword = password != null ? password.trim() : null;
    }

    public static String getHost() {
        return HOST;
    }

    public static String getPort() {
        return PORT;
    }

    public static String getUsername() {
        if (customUsername != null && !customUsername.isBlank()) {
            return customUsername;
        }
        return USERNAME;
    }

    public static String getPassword() {
        String pass = (customPassword != null && !customPassword.isBlank()) ? customPassword : PASSWORD;
        if (pass != null && pass.contains(" ") && pass.replace(" ", "").length() == 16) {
            return pass.replace(" ", "");
        }
        return pass;
    }

    public static String getFromName() {
        return FROM_NAME;
    }

    public static boolean isConfigured() {
        String u = getUsername();
        String p = getPassword();
        return u != null && !u.isBlank() && p != null && !p.isBlank();
    }

    public static Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", HOST);
        props.put("mail.smtp.port", PORT);
        props.put("mail.smtp.auth", "true");

        if ("465".equals(PORT)) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        final String activeUser = getUsername();
        final String activePass = getPassword();

        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(activeUser, activePass);
            }
        };

        return Session.getInstance(props, auth);
    }

    private static String getEnv(String key) {
        String val = System.getProperty(key);
        if (val == null || val.isBlank()) {
            val = dotenv.get(key);
        }
        if (val == null || val.isBlank()) {
            val = System.getenv(key);
        }
        if (val == null || val.isBlank()) {
            try (InputStream is = MailConfig.class.getResourceAsStream("/.env")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    val = props.getProperty(key);
                }
            } catch (Exception ignored) {}
        }
        return (val != null && !val.isBlank()) ? val.trim() : null;
    }

    private static String getEnvOrDefault(String key, String def) {
        String val = getEnv(key);
        return val != null ? val : def;
    }
}
