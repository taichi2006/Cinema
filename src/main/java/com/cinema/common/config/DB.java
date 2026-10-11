package com.cinema.common.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Quản lý kết nối Cơ sở dữ liệu:
 * - Đọc 100% từ biến môi trường hoặc file .env (DB_URL, DB_USERNAME, DB_PASSWORD)
 * - Tuyệt đối KHÔNG hardcode URL, username, password trong mã nguồn
 */
public class DB {

    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private static final String URL      = getEnv("DB_URL");
    private static final String USERNAME = getEnv("DB_USERNAME");
    private static final String PASSWORD = getEnv("DB_PASSWORD");
    private static final String DRIVER   = "org.postgresql.Driver";

    private static final EntityManagerFactory ENTITY_MANAGER_FACTORY;

    static {
        if (URL == null || USERNAME == null || PASSWORD == null) {
            throw new IllegalStateException("Thiếu cấu hình kết nối CSDL (DB_URL, DB_USERNAME, DB_PASSWORD) trong file .env hoặc biến môi trường!");
        }

        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.driver", DRIVER);
        props.put("jakarta.persistence.jdbc.url", URL);
        props.put("jakarta.persistence.jdbc.user", USERNAME);
        props.put("jakarta.persistence.jdbc.password", PASSWORD);

        ENTITY_MANAGER_FACTORY = Persistence.createEntityManagerFactory("cinemaPU", props);
    }

    private static String getEnv(String key) {
        String val = dotenv.get(key);
        if (val == null || val.isBlank()) {
            val = System.getenv(key);
        }
        if (val == null || val.isBlank()) {
            try (java.io.InputStream is = DB.class.getResourceAsStream("/.env")) {
                if (is != null) {
                    java.util.Properties props = new java.util.Properties();
                    props.load(is);
                    val = props.getProperty(key);
                }
            } catch (Exception ignored) {}
        }
        return (val != null && !val.isBlank()) ? val : null;
    }

    // 1. Cung cấp EntityManager cho JPA/Hibernate
    public static EntityManager getEntityManager() {
        return ENTITY_MANAGER_FACTORY.createEntityManager();
    }

    // 2. Cung cấp Connection cho JDBC thuần
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL Driver not found", e);
        }
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
