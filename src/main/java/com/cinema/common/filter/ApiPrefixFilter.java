package com.cinema.common.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * Cho phép gọi API qua prefix /api/*
 * Ví dụ: GET /api/movies → forward tới GET /movies
 */
@WebFilter("/api/*")
public class ApiPrefixFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI()
                .substring(httpRequest.getContextPath().length());

        if (path.startsWith("/api/")) {
            // Bỏ "/api" → forward tới servlet thực (vd: /movies)
            request.getRequestDispatcher(path.substring(4)).forward(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
