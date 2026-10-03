package com.cinema.user;

import com.cinema.auth.JwtUtil;
import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.common.exception.ErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

import static com.cinema.user.UserService.*;

@WebServlet("/user/*")
public class UserController extends HttpServlet {

    private final UserService  service = new UserService();
    private final ObjectMapper json    = new ObjectMapper();

    // ── GET /user/* ───────────────────────────────────────────────────────────

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long userId = (long) req.getAttribute("userId");
            String action = pathInfo(req);
            switch (action) {
                case "/me" -> write(resp, ApiResponse.ok(service.getMe(userId)));
                default    -> throw ApiException.notFound("Endpoint không tồn tại");
            }
        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    // ── PUT /user/* ───────────────────────────────────────────────────────────

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long userId = (long) req.getAttribute("userId");
            String action = pathInfo(req);
            switch (action) {
                case "/me" -> {
                    var body = json.readValue(req.getInputStream(), UpdateProfileRequest.class);
                    write(resp, ApiResponse.ok(service.updateProfile(userId, body), "Cập nhật thông tin thành công"));
                }
                case "/me/password" -> {
                    var body = json.readValue(req.getInputStream(), ChangePasswordRequest.class);
                    service.changePassword(userId, body);
                    write(resp, ApiResponse.success("Đổi mật khẩu thành công"));
                }
                default -> throw ApiException.notFound("Endpoint không tồn tại");
            }
        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String pathInfo(HttpServletRequest req) {
        String p = req.getPathInfo();
        if (p == null) return "/";
        if (p.endsWith("/") && p.length() > 1) {
            return p.substring(0, p.length() - 1);
        }
        return p;
    }

    private void write(HttpServletResponse resp, Object body) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        json.writeValue(resp.getWriter(), body);
    }
}
