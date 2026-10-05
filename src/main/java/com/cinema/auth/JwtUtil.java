package com.cinema.auth;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
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

    public static String generateAccessToken(long userId, String role, int authVersion) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("authVersion", authVersion)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TTL_MS))
                .signWith(KEY)
                .compact();
    }

    public static Claims parseAccessToken(String token) {
        return Jwts.parser().verifyWith(KEY).build()
                .parseSignedClaims(token).getPayload();
    }


    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isEmpty() || token.chars().anyMatch(Character::isWhitespace)) {
            return null;
        }
        return token;
    }
}
