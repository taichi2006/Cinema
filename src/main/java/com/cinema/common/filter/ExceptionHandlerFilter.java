package com.cinema.common.filter;

import com.cinema.common.config.ObjectMapperConfig;
import com.cinema.common.dto.ErrorResponse;
import com.cinema.common.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
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
        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root && !(root instanceof ApiException)) {
            root = root.getCause();
        }

        int status;
        String message;

        if (root instanceof ApiException apiEx) {
            status = apiEx.getStatus();
            message = apiEx.getMessage();
        } else if (root instanceof IllegalArgumentException || root instanceof NumberFormatException
                || root instanceof DateTimeParseException || root instanceof JsonProcessingException) {
            status = HttpServletResponse.SC_BAD_REQUEST;
            message = (root.getMessage() != null && !root.getMessage().isBlank())
                    ? root.getMessage()
                    : "Dữ liệu yêu cầu không hợp lệ";
        } else if (root instanceof NullPointerException) {
            status = HttpServletResponse.SC_BAD_REQUEST;
            message = "Thiếu trường dữ liệu bắt buộc trong yêu cầu";
        } else if (root instanceof NoResultException || root instanceof EntityNotFoundException) {
            status = HttpServletResponse.SC_NOT_FOUND;
            message = "Không tìm thấy tài nguyên yêu cầu";
        } else {
            String msg = root.getMessage() != null ? root.getMessage().toLowerCase() : "";
            if (msg.contains("users_email_key") || (msg.contains("unique") && msg.contains("email"))) {
                status = HttpServletResponse.SC_CONFLICT;
                message = "Email đã được đăng ký";
            } else if (msg.contains("rating_check")) {
                status = HttpServletResponse.SC_BAD_REQUEST;
                message = "Điểm đánh giá 'rating' phải là số nguyên từ 1 đến 5 sao.";
            } else if (msg.contains("reviews_user_id_movie_id_key")) {
                status = HttpServletResponse.SC_CONFLICT;
                message = "Bạn đã gửi đánh giá cho bộ phim này rồi.";
            } else {
                root.printStackTrace();
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                message = "Lỗi xử lý yêu cầu: " + (root.getMessage() != null ? root.getMessage() : "Đã xảy ra lỗi");
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
}
