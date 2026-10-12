package com.cinema.common.web;

import com.cinema.auth.AuthException;
import com.cinema.auth.JwtUtil;
import com.cinema.common.config.ObjectMapperConfig;
import com.cinema.common.dto.ApiResponse;
import com.cinema.common.dto.ErrorResponse;
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
        try {
            super.service(req, resp);
        } catch (Throwable e) {
            handleException(resp, e);
        }
    }

    protected void handleException(HttpServletResponse resp, Throwable ex) throws IOException {
        ApiException apiEx = findCause(ex, ApiException.class);
        int status;
        String message;

        if (apiEx != null) {
            status = apiEx.getStatus();
            message = apiEx.getMessage();
        } else {
            String fullMessage = buildFullMessage(ex).toLowerCase();

            if (fullMessage.contains("users_email_key") || (fullMessage.contains("unique") && fullMessage.contains("email"))) {
                status = HttpServletResponse.SC_CONFLICT;
                message = "Email đã được đăng ký";
            } else if (fullMessage.contains("rating_check")) {
                status = HttpServletResponse.SC_BAD_REQUEST;
                message = "Điểm đánh giá 'rating' phải là số nguyên từ 1 đến 5 sao.";
            } else if (fullMessage.contains("reviews_user_id_movie_id_key")) {
                status = HttpServletResponse.SC_CONFLICT;
                message = "Bạn đã gửi đánh giá cho bộ phim này rồi.";
            } else if (findCause(ex, IllegalArgumentException.class) != null) {
                IllegalArgumentException iae = findCause(ex, IllegalArgumentException.class);
                status = HttpServletResponse.SC_BAD_REQUEST;
                message = (iae != null && iae.getMessage() != null && !iae.getMessage().isBlank())
                        ? iae.getMessage()
                        : "Dữ liệu yêu cầu không hợp lệ";
            } else if (findCause(ex, NullPointerException.class) != null) {
                status = HttpServletResponse.SC_BAD_REQUEST;
                message = "Thiếu trường dữ liệu bắt buộc trong yêu cầu";
            } else if (findCause(ex, NumberFormatException.class) != null) {
                status = HttpServletResponse.SC_BAD_REQUEST;
                message = "Định dạng số không hợp lệ";
            } else if (findCause(ex, DateTimeParseException.class) != null) {
                status = HttpServletResponse.SC_BAD_REQUEST;
                message = "Định dạng ngày giờ không hợp lệ";
            } else if (findCause(ex, JsonProcessingException.class) != null) {
                status = HttpServletResponse.SC_BAD_REQUEST;
                message = "Dữ liệu JSON không hợp lệ";
            } else if (findCause(ex, jakarta.persistence.NoResultException.class) != null
                    || findCause(ex, jakarta.persistence.EntityNotFoundException.class) != null) {
                status = HttpServletResponse.SC_NOT_FOUND;
                message = "Không tìm thấy tài nguyên yêu cầu";
            } else {
                ex.printStackTrace();
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                message = "Lỗi xử lý yêu cầu: " + (ex.getMessage() != null ? ex.getMessage() : "Đã xảy ra lỗi");
            }
        }

        if (!resp.isCommitted()) {
            try {
                resp.resetBuffer();
            } catch (IllegalStateException ignored) {}
            resp.setStatus(status);
            resp.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(resp.getWriter(), new ErrorResponse(status, message));
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Throwable> T findCause(Throwable ex, Class<T> clazz) {
        Throwable curr = ex;
        while (curr != null) {
            if (clazz.isInstance(curr)) {
                return (T) curr;
            }
            if (curr.getCause() == curr) break;
            curr = curr.getCause();
        }
        return null;
    }

    private static String buildFullMessage(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        Throwable curr = ex;
        while (curr != null) {
            if (curr.getMessage() != null) {
                sb.append(" ").append(curr.getMessage());
            }
            if (curr.getCause() == curr) break;
            curr = curr.getCause();
        }
        return sb.toString();
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
            T body = mapper.readValue(req.getInputStream(), clazz);
            if (body == null) {
                throw ApiException.badRequest("Dữ liệu yêu cầu không được để trống");
            }
            return body;
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
