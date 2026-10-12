package com.cinema.payment.gateway;

import com.cinema.common.exception.ApiException;
import io.github.cdimascio.dotenv.Dotenv;

import java.time.ZoneId;

public record VnPayConfig(
        String tmnCode,
        String hashSecret,
        String paymentUrl,
        String returnUrl,
        ZoneId zoneId) {

    public static VnPayConfig fromEnvironment() {
        return new VnPayConfig(
                required("VNPAY_TMN_CODE"),
                required("VNPAY_HASH_SECRET"),
                required("VNPAY_PAYMENT_URL"),
                required("VNPAY_RETURN_URL"),
                ZoneId.of(valueOrDefault("VNPAY_TIME_ZONE", "Asia/Ho_Chi_Minh"))
        );
    }

    private static String required(String name) {
        String value = environmentValue(name);
        if (value == null || value.isBlank()) {
            throw ApiException.internal("Thiếu cấu hình môi trường " + name + " cho VNPAY");
        }
        return value.trim();
    }

    private static String valueOrDefault(String name, String defaultValue) {
        String value = environmentValue(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private static String environmentValue(String name) {
        String value = System.getenv(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = System.getProperty(name);
        if (value != null && !value.isBlank()) {
            return value;
        }
        try {
            return Dotenv.configure().ignoreIfMissing().load().get(name);
        } catch (Exception ignored) {
            return null;
        }
    }
}
