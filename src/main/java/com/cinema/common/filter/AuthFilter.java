package com.cinema.common.filter;

import com.cinema.auth.AuthException;
import com.cinema.auth.JwtUtil;
import com.cinema.common.exception.ApiException;
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
        "/api/booking", "/api/booking/*", "/api/bookings", "/api/bookings/*",
        "/api/wallet", "/api/wallet/*",
        "/api/v1/booking", "/api/v1/booking/*", "/api/v1/bookings", "/api/v1/bookings/*",
        "/api/v1/wallet", "/api/v1/wallet/*",
        "/api/v1/auth/logout", "/api/auth/logout", "/auth/logout"
}, dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

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

        } catch (JwtException | IllegalArgumentException e) {
            throw new ServletException(AuthException.invalidToken());
        } catch (Exception ex) {
            throw new ServletException(ex);
        }
    }
}
