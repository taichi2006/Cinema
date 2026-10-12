package com.cinema.common.util;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;

public class JPAUtil {

    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private static final EntityManagerFactory emf;

    static {
        String url = getEnv("DB_URL");
        String username = getEnv("DB_USERNAME");
        String password = getEnv("DB_PASSWORD");

        if (url == null || username == null || password == null) {
            throw new IllegalStateException("Thiếu cấu hình kết nối CSDL trong .env hoặc biến môi trường!");
        }

        Map<String, Object> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
        props.put("jakarta.persistence.jdbc.url", url);
        props.put("jakarta.persistence.jdbc.user", username);
        props.put("jakarta.persistence.jdbc.password", password);

        emf = Persistence.createEntityManagerFactory("cinemaPU", props);
    }

    private static String getEnv(String key) {
        String val = dotenv.get(key);
        return (val != null && !val.isBlank()) ? val : System.getenv(key);
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }
}
