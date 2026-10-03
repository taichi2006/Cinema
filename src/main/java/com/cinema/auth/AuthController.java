package com.cinema.auth;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.common.exception.ErrorHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

import static com.cinema.auth.AuthService.*;

@WebServlet("/auth/*")
public class AuthController extends HttpServlet {

    private final AuthService  service = new AuthService();
    private final ObjectMapper json    = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        String action = req.getPathInfo() == null ? "/" : req.getPathInfo();
        if (action.endsWith("/") && action.length() > 1) {
            action = action.substring(0, action.length() - 1);
        }

        try {
            switch (action) {
                case "/register"        -> doRegister(req, resp);
                case "/login"           -> doLogin(req, resp);
                case "/logout"          -> doLogout(req, resp);
                default -> throw ApiException.notFound("Endpoint không tồn tại");
            }
        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void doRegister(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        var body = json.readValue(req.getInputStream(), RegisterRequest.class);
        AuthUserResponse user = service.register(body);
        resp.setStatus(201);
        write(resp, ApiResponse.ok(user, "Đăng ký thành công"));
    }

    private void doLogin(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        var body   = json.readValue(req.getInputStream(), LoginRequest.class);
        LoginData result = service.login(body);
        
        // Cấp Access Token dài ngày
        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_ACCESS, result.accessToken(), 7 * 24 * 3600));
        write(resp, ApiResponse.ok(result, "Đăng nhập thành công"));
    }

    private void doLogout(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        resp.addCookie(JwtUtil.buildCookie(JwtUtil.COOKIE_ACCESS, "", 0));
        write(resp, ApiResponse.success("Đăng xuất thành công"));
    }

    // ── JSON helper ───────────────────────────────────────────────────────────

    private void write(HttpServletResponse resp, Object body) throws IOException {
        json.writeValue(resp.getWriter(), body);
    }
}
