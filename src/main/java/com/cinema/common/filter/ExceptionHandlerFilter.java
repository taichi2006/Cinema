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
        Throwable cause = ex;
        while (cause instanceof ServletException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        int status;
        String message;

        if (cause instanceof ApiException apiEx) {
            status = apiEx.getStatus();
            message = apiEx.getMessage();
        } else if (cause instanceof IllegalArgumentException) {
            status = 400;
            message = (cause.getMessage() != null && !cause.getMessage().isBlank())
                    ? cause.getMessage()
                    : "Dữ liệu yêu cầu không hợp lệ";
        } else if (cause instanceof NullPointerException) {
            status = 400;
            message = "Thiếu trường dữ liệu bắt buộc trong yêu cầu";
        } else if (cause instanceof NumberFormatException) {
            status = 400;
            message = "Định dạng số không hợp lệ";
        } else if (cause instanceof DateTimeParseException) {
            status = 400;
            message = "Định dạng ngày giờ không hợp lệ";
        } else if (cause instanceof JsonProcessingException) {
            status = 400;
            message = "Dữ liệu JSON không hợp lệ";
        } else {
            cause.printStackTrace();
            status = 500;
            message = "Lỗi xử lý yêu cầu: " + (cause.getMessage() != null ? cause.getMessage() : "Đã xảy ra lỗi");
        }

        if (!resp.isCommitted()) {
            resp.setStatus(status);
            resp.setContentType("application/json;charset=UTF-8");
            json.writeValue(resp.getWriter(), new ErrorResponse(status, message));
        }
    }
}
