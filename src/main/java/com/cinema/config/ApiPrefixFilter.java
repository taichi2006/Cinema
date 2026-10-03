package com.cinema.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

@WebFilter("/api/*")
public class ApiPrefixFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());

        if (path.startsWith("/api/")) {
            // Loại bỏ chuỗi "/api" ra khỏi path để forward tới các Servlet thực tế (ví dụ: "/movie")
            String targetPath = path.substring(4); 
            request.getRequestDispatcher(targetPath).forward(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
