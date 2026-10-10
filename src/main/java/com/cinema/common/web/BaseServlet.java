package com.cinema.common.web;

import com.cinema.common.config.ObjectMapperConfig;
import com.cinema.common.dto.ApiResponse;
import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public abstract class BaseServlet extends HttpServlet {
    protected final ObjectMapper mapper = ObjectMapperConfig.getInstance();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json;charset=UTF-8");
        super.service(req, resp);
    }

    // ========== GHI RESPONSE ==========
    protected void sendJson(HttpServletResponse resp, Object data) throws IOException {
        mapper.writeValue(resp.getWriter(), data);
    }

    protected <T> void writeSuccess(HttpServletResponse resp, T data) throws IOException {
        sendJson(resp, ApiResponse.ok(data));
    }

    protected void writeMessage(HttpServletResponse resp, String message) throws IOException {
        sendJson(resp, ApiResponse.success(message));
    }

    protected <T> void writePage(HttpServletResponse resp, List<T> data, PageMeta meta) throws IOException {
        sendJson(resp, ApiResponse.ok(PageResponse.of(data, meta)));
    }

    // ========== ĐỌC REQUEST ==========
    protected <T> T parseBody(HttpServletRequest req, Class<T> clazz) throws IOException {
        return mapper.readValue(req.getInputStream(), clazz);
    }

    protected int getIntParam(HttpServletRequest req, String name, int defaultValue) {
        String value = req.getParameter(name);
        if (value == null || value.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected PageMeta getPagination(HttpServletRequest req, long totalElements) {
        int page = getIntParam(req, "page", 1);
        int size = getIntParam(req, "size", 10);
        if (size <= 0) size = 10;
        return new PageMeta(page, size, totalElements);
    }

    // ========== XÁC THỰC ==========
    protected long getAuthenticatedUserId(HttpServletRequest request) {
        Object attribute = request.getAttribute("userId");
        if (attribute instanceof Number userId) {
            return userId.longValue();
        }
        throw com.cinema.auth.AuthException.unauthorized();
    }

    protected Long getOptionalUserId(HttpServletRequest request) {
        try {
            return getAuthenticatedUserId(request);
        } catch (com.cinema.auth.AuthException e) {
            return null;
        }
    }

    // ========== PATH PARSING ==========
    protected String[] getPathSegments(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/") || pathInfo.trim().isEmpty()) {
            return new String[0];
        }
        return java.util.Arrays.stream(pathInfo.split("/"))
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }

    protected long parsePathId(String idStr, String name) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw com.cinema.common.exception.ApiException.badRequest(name + " không được để trống.");
        }
        try {
            long id = Long.parseLong(idStr.trim());
            if (id <= 0) {
                throw com.cinema.common.exception.ApiException.badRequest(name + " phải là số nguyên dương.");
            }
            return id;
        } catch (NumberFormatException e) {
            throw com.cinema.common.exception.ApiException.badRequest(name + " không hợp lệ: " + idStr);
        }
    }
}
