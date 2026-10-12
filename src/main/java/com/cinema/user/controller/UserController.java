package com.cinema.user.controller;

import com.cinema.auth.AuthService;
import com.cinema.auth.dto.request.ChangePasswordRequest;
import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import com.cinema.user.dto.request.UpdateUserRequest;
import com.cinema.user.service.UserBookingService;
import com.cinema.user.service.UserService;
import com.cinema.user.service.UserVoucherService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;

@WebServlet(urlPatterns = { "/user", "/user/*" })
public class UserController extends BaseServlet {

    private final UserService service = new UserService();
    private final UserBookingService bookingService = new UserBookingService();
    private final UserVoucherService voucherService = new UserVoucherService();
    private final AuthService authService = new AuthService();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(req.getMethod())) {
            doPatch(req, resp);
            return;
        }
        super.service(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // GET /user (Thông tin tài khoản)
        if (segments.length == 0) {
            writeSuccess(resp, service.getProfile(userId));
            return;
        }

        // GET /user/bookings hoặc /user/vouchers
        if (segments.length == 1) {
            String sub = segments[0].toLowerCase();

            if ("bookings".equals(sub)) {
                String status = req.getParameter("status");
                LocalDate from = parseDateParam(req, "from");
                LocalDate to = parseDateParam(req, "to");
                int page = getIntParam(req, "page", 0);
                int size = getIntParam(req, "size", 20);
                String sort = req.getParameter("sort");

                writeSuccess(resp, bookingService.getBookingHistory(userId, status, from, to, page, size, sort));
                return;
            }

            if ("vouchers".equals(sub)) {
                String status = req.getParameter("status");
                int page = getIntParam(req, "page", 0);
                int size = getIntParam(req, "size", 20);

                writeSuccess(resp, voucherService.getUserVouchers(userId, status, page, size));
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại: " + sub);
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }

    @Override
    protected void doPatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // PATCH /user (Cập nhật thông tin)
        if (segments.length == 0) {
            UpdateUserRequest body = parseBody(req, UpdateUserRequest.class);
            writeSuccess(resp, service.updateProfile(userId, body));
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // POST /user/change-password (Đổi mật khẩu)
        if (segments.length == 1 && "change-password".equalsIgnoreCase(segments[0])) {
            ChangePasswordRequest body = parseBody(req, ChangePasswordRequest.class);
            authService.changePassword(userId, body);
            writeSuccess(resp, null);
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}
