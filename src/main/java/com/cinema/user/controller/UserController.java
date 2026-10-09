package com.cinema.user.controller;

import com.cinema.user.dto.request.UpdateUserRequest;
import com.cinema.user.dto.request.UserBookingRequest;
import com.cinema.user.dto.request.UserVoucherRequest;
import com.cinema.user.service.UserBookingService;
import com.cinema.user.service.UserService;
import com.cinema.user.service.UserVoucherService;

import com.cinema.auth.AuthService;
import com.cinema.auth.JwtUtil;
import com.cinema.auth.dto.request.ChangePasswordRequest;
import com.cinema.auth.AuthException;
import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.common.exception.ErrorHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet(urlPatterns = {"/user", "/user/*"})
public class UserController extends HttpServlet {

    private final UserService service = new UserService();
    private final UserBookingService bookingService = new UserBookingService();
    private final UserVoucherService voucherService = new UserVoucherService();
    private final AuthService authService = new AuthService();
    private final ObjectMapper json = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
            return;
        }
        super.service(req, resp);
    }

    // GET /user, /user/bookings, /user/vouchers

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long userId = authenticatedUserId(req);
            String action = pathInfo(req);
            switch (action) {
                case "", "/" -> write(resp, ApiResponse.ok(service.getProfile(userId)));
                case "/bookings", "/booking" -> write(resp, ApiResponse.ok(
                        bookingService.getBookingHistory(
                                userId,
                                new UserBookingRequest(
                                        req.getParameter("status"),
                                        req.getParameter("from"),
                                        req.getParameter("to"),
                                        req.getParameter("page"),
                                        req.getParameter("size"),
                                        req.getParameter("sort")
                                )
                        )
                ));
                case "/vouchers", "/voucher" -> write(resp, ApiResponse.ok(
                        voucherService.getUserVouchers(
                                userId,
                                new UserVoucherRequest(
                                        req.getParameter("status"),
                                        req.getParameter("page"),
                                        req.getParameter("size")
                                )
                        )
                ));
                default -> throw ApiException.notFound("Endpoint không tồn tại");
            }
        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    // PATCH /user

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long userId = authenticatedUserId(req);
            String action = pathInfo(req);
            switch (action) {
                case "", "/" -> {
                    var body = readJson(req, UpdateUserRequest.class);
                    write(resp, ApiResponse.ok(service.updateProfile(userId, body)));
                }
                default -> throw ApiException.notFound("Endpoint không tồn tại");
            }
        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    // POST /user/change-password

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long userId = authenticatedUserId(req);
            String action = pathInfo(req);
            if (!"/change-password".equals(action)) {
                throw ApiException.notFound("Endpoint không tồn tại");
            }

            var body = readJson(req, ChangePasswordRequest.class);
            authService.changePassword(userId, body);
            write(resp, ApiResponse.ok(null));
        } catch (Exception exception) {
            ErrorHandler.handle(resp, exception);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String pathInfo(HttpServletRequest req) {
        String p = req.getPathInfo();
        if (p == null || p.isBlank()) {
            return "/";
        }
        if (p.endsWith("/") && p.length() > 1) {
            return p.substring(0, p.length() - 1);
        }
        return p;
    }

    private long authenticatedUserId(HttpServletRequest request) {
        Object attribute = request.getAttribute("userId");
        if (attribute instanceof Number userId) {
            return userId.longValue();
        }

        String token = null;
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }
        if (token == null || token.isBlank()) {
            token = JwtUtil.readCookie(request, JwtUtil.COOKIE_ACCESS);
        }
        if (token != null && !token.isBlank()) {
            try {
                Claims claims = JwtUtil.parseAccessToken(token);
                return Long.parseLong(claims.getSubject());
            } catch (Exception ignored) {}
        }
        throw AuthException.unauthorized();
    }

    private <T> T readJson(HttpServletRequest request, Class<T> type) throws IOException {
        try {
            return json.readValue(request.getInputStream(), type);
        } catch (JsonProcessingException exception) {
            throw ApiException.badRequest("JSON không hợp lệ");
        }
    }

    private void write(HttpServletResponse resp, Object body) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        json.writeValue(resp.getWriter(), body);
    }
}
