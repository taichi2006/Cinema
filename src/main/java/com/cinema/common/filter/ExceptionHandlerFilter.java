package com.cinema.common.filter;

import com.cinema.common.config.ObjectMapperConfig;
import com.cinema.common.dto.ErrorResponse;
import com.cinema.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.format.DateTimeParseException;

@WebFilter(urlPatterns = "/*", dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ERROR})
public class ExceptionHandlerFilter implements Filter {
    private final ObjectMapper json = ObjectMapperConfig.getInstance();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (Throwable ex) {
            handleException((HttpServletResponse) response, ex);
        }
    }

    private void handleException(HttpServletResponse resp, Throwable ex) throws IOException {
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
            json.writeValue(resp.getWriter(), new ErrorResponse(status, message));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> T findCause(Throwable ex, Class<T> clazz) {
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
}
