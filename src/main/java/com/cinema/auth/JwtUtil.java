package com.cinema.auth;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;

public class JwtUtil {

    private static final long ACCESS_TTL_MS  = 15 * 60 * 1_000L;
    private static final long REFRESH_TTL_MS = 7L * 24 * 60 * 60 * 1_000L;
    private static final long RESET_TTL_MS   = 60 * 60 * 1_000L;

    public static final String COOKIE_ACCESS  = "access_token";
    public static final String COOKIE_REFRESH = "refresh_token";

    private static final SecretKey KEY;
    private static final SecureRandom RNG = new SecureRandom();

    static {
        Dotenv dotenv = Dotenv.configure().directory("f:/ltw/cinema").ignoreIfMissing().load();
        String secret = dotenv.get("JWT_SECRET");
        if (secret == null) {
            secret = "fallback-secret-key-for-development-must-be-at-least-256-bits-long-2024!";
        }
        KEY = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ── Access Token ──────────────────────────────────────────────────────────

    public static String generateAccessToken(long userId, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
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
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
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
