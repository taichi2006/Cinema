package com.cinema.common.filter;

import com.cinema.auth.AuthException;
import com.cinema.auth.AuthService;
import com.cinema.auth.JwtUtil;
import com.cinema.common.exception.ApiException;
import com.cinema.common.exception.ErrorHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(urlPatterns = {"/user/*", "/booking/*", "/admin/*"}, dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD})
public class AuthFilter implements Filter {

    private final AuthService authService = new AuthService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        try {
            String token = JwtUtil.extractBearerToken(req.getHeader("Authorization"));
            if (token == null)
                throw AuthException.unauthorized();

            Claims claims = JwtUtil.parseAccessToken(token);
            long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);
            Number authVersionClaim = claims.get("authVersion", Number.class);
            if (role == null || authVersionClaim == null) {
                throw AuthException.invalidToken();
            }

            int authVersion = authVersionClaim.intValue();
            authService.validateAccessIdentity(userId, role, authVersion);

            // Lưu vào Request Attribute để các Controller phía sau sử dụng
            req.setAttribute("userId", userId);
            req.setAttribute("role", role);
            req.setAttribute("authVersion", authVersion);

            chain.doFilter(request, response);

        } catch (JwtException | IllegalArgumentException e) {
            ErrorHandler.handle(resp, AuthException.invalidToken());
        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }
}
