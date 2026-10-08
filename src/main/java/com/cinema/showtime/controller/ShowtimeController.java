package com.cinema.showtime;

import com.cinema.auth.AuthException;
import com.cinema.auth.JwtUtil;
import com.cinema.common.exception.ApiException;
import com.cinema.common.exception.ErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@WebServlet(urlPatterns = {"/showtime", "/showtime/*"})
public class ShowtimeController extends HttpServlet {

    private final ShowtimeService showtimeService = new ShowtimeService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.trim().isEmpty()) {
                throw ApiException.notFound("Endpoint không tồn tại.");
            }

            String[] segments = Arrays.stream(pathInfo.split("/"))
                    .filter(s -> !s.isBlank())
                    .toArray(String[]::new);

            if (segments.length == 2) {
                String id = segments[0];
                String subResource = segments[1].toLowerCase();
                switch (subResource) {
                    case "seat", "seats" -> handleGetSeatMap(request, response, id);
                    default -> throw ApiException.notFound("Endpoint không tồn tại: " + subResource);
                }
            } else {
                throw ApiException.notFound("Đường dẫn không hợp lệ.");
            }

        } catch (Exception ex) {
            ErrorHandler.handle(response, ex);
        }
    }

    private void handleGetSeatMap(HttpServletRequest request, HttpServletResponse response, String id) throws Exception {
        Long userId = resolveUserId(request);
        Map<String, Object> result = showtimeService.getSeatMap(id, userId);

        response.setStatus(HttpServletResponse.SC_OK);
        objectMapper.writeValue(response.getWriter(), result);
    }

    private Long resolveUserId(HttpServletRequest request) {
        Object attrUserId = request.getAttribute("userId");
        if (attrUserId instanceof Number num) {
            return num.longValue();
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = JwtUtil.parseAccessToken(token);
                return Long.parseLong(claims.getSubject());
            } catch (JwtException | IllegalArgumentException e) {
                throw AuthException.invalidToken();
            }
        }

        throw AuthException.unauthorized();
    }
}
