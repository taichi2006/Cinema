package com.cinema.common.util;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

/**
 * Tiện ích JPA quản lý EntityManagerFactory và EntityManager:
 * - Đọc cấu hình kết nối CSDL từ file .env hoặc biến môi trường hệ thống
 * - Cung cấp EntityManager cho toàn bộ các DAO / Service
 */
public class JPAUtil {

    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    private static final String URL      = getEnv("DB_URL");
    private static final String USERNAME = getEnv("DB_USERNAME");
    private static final String PASSWORD = getEnv("DB_PASSWORD");
    private static final String DRIVER   = "org.postgresql.Driver";

    private static final EntityManagerFactory emf;

    static {
        if (URL == null || USERNAME == null || PASSWORD == null) {
            throw new IllegalStateException("Thiếu cấu hình kết nối CSDL (DB_URL, DB_USERNAME, DB_PASSWORD) trong file .env hoặc biến môi trường!");
        }

        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.driver", DRIVER);
        props.put("jakarta.persistence.jdbc.url", URL);
        props.put("jakarta.persistence.jdbc.user", USERNAME);
        props.put("jakarta.persistence.jdbc.password", PASSWORD);

        emf = Persistence.createEntityManagerFactory("cinemaPU", props);
    }

    private static String getEnv(String key) {
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
        return (val != null && !val.isBlank()) ? val.trim() : null;
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void shutdown() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
