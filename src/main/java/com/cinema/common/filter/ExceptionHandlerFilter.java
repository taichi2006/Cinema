package com.cinema.common.filter;

import com.cinema.common.config.ObjectMapperConfig;
import com.cinema.common.dto.ErrorResponse;
import com.cinema.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(urlPatterns = "/*")
public class ExceptionHandlerFilter implements Filter {
    private final ObjectMapper json = ObjectMapperConfig.getInstance();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (Exception ex) {
            handleException((HttpServletResponse) response, ex);
        }
    }

    private void handleException(HttpServletResponse resp, Exception ex) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int status = 500;
        String message = "Lỗi server nội bộ";

        // Tìm cause thực sự nếu bị bọc bởi ServletException
        Throwable cause = ex;
        while (cause instanceof ServletException && cause.getCause() != null) {
            cause = cause.getCause();
        }

        if (cause instanceof ApiException) {
            ApiException apiEx = (ApiException) cause;
            status = apiEx.getStatus();
            message = apiEx.getMessage();
        } else {
            cause.printStackTrace();
            message = "Lỗi server nội bộ: " + cause.getMessage();
        }

        resp.setStatus(status);
        json.writeValue(resp.getWriter(), new ErrorResponse(status, message));
    }
}
