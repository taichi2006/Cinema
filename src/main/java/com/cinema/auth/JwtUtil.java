package com.cinema.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    private static final long ACCESS_TTL_MS  = 15 * 60 * 1_000L;
    private static final long REFRESH_TTL_MS = 7L * 24 * 60 * 60 * 1_000L;
    private static final long RESET_TTL_MS   = 60 * 60 * 1_000L;

    public static final String COOKIE_ACCESS  = "access_token";
    public static final String COOKIE_REFRESH = "refresh_token";

    private static final SecretKey KEY;

    static {
        io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                .ignoreIfMissing()
                .load();
        String secret = dotenv.get("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = System.getenv("JWT_SECRET");
        }
        if (secret == null || secret.isBlank()) {
            try (java.io.InputStream is = JwtUtil.class.getResourceAsStream("/.env")) {
                if (is != null) {
                    java.util.Properties props = new java.util.Properties();
                    props.load(is);
                    secret = props.getProperty("JWT_SECRET");
                }
            } catch (Exception ignored) {}
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Thiếu cấu hình JWT_SECRET trong file .env hoặc biến môi trường!");
        }
        KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public static int getAccessTtlSeconds() {
        return (int) (ACCESS_TTL_MS / 1000L);
    }

    public static int getRefreshTtlSeconds() {
        return (int) (REFRESH_TTL_MS / 1000L);
    }

    // ── Access Token ──────────────────────────────────────────────────────────

    public static String generateAccessToken(long userId) {
        return generateAccessToken(userId, 1);
    }

    public static String generateAccessToken(long userId, int tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("token_version", tokenVersion)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TTL_MS))
                .signWith(KEY)
                .compact();
    }

    public static Claims parseAccessToken(String token) {
        return Jwts.parser().verifyWith(KEY).build()
                .parseSignedClaims(token).getPayload();
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    public static String generateRefreshToken(long userId) {
        return generateRefreshToken(userId, 1);
    }

    public static String generateRefreshToken(long userId, int tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .claim("token_version", tokenVersion)
                .id(java.util.UUID.randomUUID().toString())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + REFRESH_TTL_MS))
                .signWith(KEY)
                .compact();
    }

    public static Claims parseRefreshToken(String token) {
        Claims claims = Jwts.parser().verifyWith(KEY).build()
                .parseSignedClaims(token).getPayload();
        if (!"refresh".equals(claims.get("type", String.class))) {
            throw new JwtException("Invalid token type");
        }
        return claims;
    }

    // ── Cookie helpers (dùng chung) ───────────────────────────────────────────

    public static String readCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) if (name.equals(c.getName())) return c.getValue();
        return null;
    }

    public static Cookie buildCookie(String name, String value, int maxAge) {
        Cookie c = new Cookie(name, value);
        c.setHttpOnly(true);
        c.setSecure(false); // → true khi dùng HTTPS
        c.setPath("/");
        c.setMaxAge(maxAge);
        c.setAttribute("SameSite", "Lax");
        return c;
    }
}
