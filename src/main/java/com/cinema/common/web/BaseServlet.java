package com.cinema.common.web;

import com.cinema.auth.AuthException;
import com.cinema.auth.JwtUtil;
import com.cinema.common.config.ObjectMapperConfig;
import com.cinema.common.dto.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

public abstract class BaseServlet extends HttpServlet {

    protected final ObjectMapper mapper = ObjectMapperConfig.getInstance();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        super.service(req, resp);
    }

    // 1. Phản hồi JSON
    protected void sendJson(HttpServletResponse resp, Object data) throws IOException {
        mapper.writeValue(resp.getWriter(), data);
    }

    protected <T> void writeSuccess(HttpServletResponse resp, T data) throws IOException {
        sendJson(resp, ApiResponse.ok(data));
    }

    // 2. Đọc Request Body & Param
    protected <T> T parseBody(HttpServletRequest req, Class<T> clazz) throws IOException {
        try {
            return mapper.readValue(req.getInputStream(), clazz);
        } catch (JsonProcessingException e) {
            throw ApiException.badRequest("Dữ liệu JSON không hợp lệ: " + e.getOriginalMessage());
        }
    }

    protected <T> T parseOptionalBody(HttpServletRequest req, Class<T> clazz) {
        try {
            if (req.getContentLengthLong() <= 0) return null;
            return mapper.readValue(req.getInputStream(), clazz);
        } catch (Exception e) {
            return null;
        }
    }

    protected int getIntParam(HttpServletRequest req, String name, int defaultValue) {
        String value = req.getParameter(name);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected LocalDate parseDateParam(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Tham số '" + name + "' phải có định dạng YYYY-MM-DD: " + value);
        }
    }

    // 3. Phân tích đường dẫn (Routing & Path Variables)
    protected String[] getPathSegments(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isBlank()) return new String[0];
        return Arrays.stream(pathInfo.split("/")).filter(s -> !s.isBlank()).toArray(String[]::new);
    }

    protected long parsePathId(String idStr, String name) {
        if (idStr == null || idStr.isBlank()) {
            throw ApiException.badRequest(name + " không được để trống.");
        }
        try {
            long id = Long.parseLong(idStr.trim());
            if (id <= 0) throw ApiException.badRequest(name + " phải là số nguyên dương.");
            return id;
        } catch (NumberFormatException e) {
            throw ApiException.badRequest(name + " không hợp lệ: " + idStr);
        }
    }

    // 4. Xác thực người dùng (JWT)
    protected long getAuthenticatedUserId(HttpServletRequest req) {
        Long id = getOptionalUserId(req);
        if (id != null) return id;
        throw AuthException.unauthorized();
    }

    protected Long getOptionalUserId(HttpServletRequest req) {
        Object attr = req.getAttribute("userId");
        if (attr instanceof Number n) return n.longValue();

        try {
            String header = req.getHeader("Authorization");
            String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
            if (token == null) {
                token = JwtUtil.readCookie(req, JwtUtil.COOKIE_ACCESS);
            }
            if (token != null && !token.isBlank()) {
                Claims claims = JwtUtil.parseAccessToken(token);
                long userId = Long.parseLong(claims.getSubject());
                req.setAttribute("userId", userId);
                return userId;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
