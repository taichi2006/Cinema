package com.cinema.common.filter;

import com.cinema.auth.dao.AuthDAO;
import com.cinema.common.util.JwtUtil;
import com.cinema.common.config.ObjectMapperConfig;
import com.cinema.common.dto.ErrorResponse;
import com.cinema.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;

@WebFilter(urlPatterns = {
        "/user", "/user/*",
        "/bookings", "/bookings/*",
        "/wallet", "/wallet/*",
        "/payments", "/payments/*",
        "/tickets", "/tickets/*",
        "/auth/logout",
        "/auth/change-password"
}, dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class AuthFilter implements Filter {

    private final ObjectMapper mapper = ObjectMapperConfig.getInstance();
    private final AuthDAO authDAO = new AuthDAO();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // IPN và Return URL là callback từ VNPAY và được xác thực bằng checksum.
        String uri = req.getRequestURI();
        if (uri.endsWith("/payments/webhook") || uri.contains("/payments/vnpay-return")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String authHeader = req.getHeader("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            if (token == null) {
                token = JwtUtil.readCookie(req, JwtUtil.COOKIE_ACCESS);
            }
            if (token == null || token.isBlank()) {
                sendError(resp, 401, "Chưa đăng nhập");
                return;
            }

            Claims claims = JwtUtil.parseAccessToken(token);
            long userId = Long.parseLong(claims.getSubject());
            Integer tokenVer = claims.get("token_version", Integer.class);

            // Đảm bảo chỉ 1 thiết bị đăng nhập tại một thời điểm (pure JWT Token Version)
            if (tokenVer == null || !authDAO.isTokenVersionValid(userId, tokenVer)) {
                sendError(resp, 401, "Tài khoản của bạn đã được đăng nhập từ một thiết bị khác");
                return;
            }

            // Lưu vào Request Attribute để các Controller phía sau sử dụng
            req.setAttribute("userId", userId);

            chain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException e) {
            sendError(resp, 401, "Access token không hợp lệ hoặc đã hết hạn");
        } catch (ApiException apiEx) {
            sendError(resp, apiEx.getStatus(), apiEx.getMessage());
        } catch (Exception ex) {
            sendError(resp, 500, "Lỗi xác thực người dùng: " + ex.getMessage());
        }
    }

    private void sendError(HttpServletResponse resp, int status, String message) throws IOException {
        if (!resp.isCommitted()) {
            resp.setStatus(status);
            resp.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(resp.getWriter(), new ErrorResponse(status, message));
        }
    }
}
