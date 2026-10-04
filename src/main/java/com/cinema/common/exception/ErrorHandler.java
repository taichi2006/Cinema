package com.cinema.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.cinema.common.dto.CommonDTO.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class ErrorHandler {
    private static final ObjectMapper json = new ObjectMapper();

    public static void handle(HttpServletResponse resp, Exception ex) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        int status = 500;
        String message = "Lỗi server nội bộ";

        if (ex instanceof ApiException apiEx) {
            status = apiEx.getStatus();
            message = apiEx.getMessage();
        } else {
            ex.printStackTrace();
            message = "Lỗi server nội bộ: " + ex.getMessage();
        }

        resp.setStatus(status);
        json.writeValue(resp.getWriter(), new ErrorResponse(status, message));
    }
}
