package com.cinema.common.filter;

import com.cinema.auth.AuthException;
import com.cinema.auth.JwtUtil;
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
        "/booking", "/booking/*", "/bookings", "/bookings/*",
        "/wallet", "/wallet/*",
        "/payments", "/payments/*",
        "/tickets", "/tickets/*",
        "/api/booking", "/api/booking/*", "/api/bookings", "/api/bookings/*",
        "/api/wallet", "/api/wallet/*",
        "/api/payments", "/api/payments/*",
        "/api/tickets", "/api/tickets/*",
        "/api/v1/booking", "/api/v1/booking/*", "/api/v1/bookings", "/api/v1/bookings/*",
        "/api/v1/wallet", "/api/v1/wallet/*",
        "/api/v1/payments", "/api/v1/payments/*",
        "/api/v1/tickets", "/api/v1/tickets/*",
        "/api/v1/auth/logout", "/api/auth/logout", "/auth/logout"
}, dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class AuthFilter implements Filter {

    private final ObjectMapper json = ObjectMapperConfig.getInstance();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // IPN là callback server-to-server của VNPAY và được xác thực bằng checksum.
        // Request URI vẫn giữ nguyên khi ApiPrefixFilter forward nên kiểm tra suffix
        // hoạt động cho cả /payments, /api/payments và /api/v1/payments.
        if (req.getRequestURI().endsWith("/payments/webhook")) {
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
            if (token == null)
                throw AuthException.unauthorized();

            Claims claims = JwtUtil.parseAccessToken(token);
            long userId = Long.parseLong(claims.getSubject());

            // Lưu vào Request Attribute để các Controller phía sau sử dụng
            req.setAttribute("userId", userId);

            chain.doFilter(request, response);

        } catch (AuthException exception) {
            writeAuthError(resp, exception);
        } catch (JwtException | IllegalArgumentException e) {
            writeAuthError(resp, AuthException.invalidToken());
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }

    private void writeAuthError(HttpServletResponse response, AuthException exception) throws IOException {
        response.setStatus(exception.getStatus());
        response.setContentType("application/json;charset=UTF-8");
        json.writeValue(response.getWriter(), new ErrorResponse(exception.getStatus(), exception.getMessage()));
    }
}
