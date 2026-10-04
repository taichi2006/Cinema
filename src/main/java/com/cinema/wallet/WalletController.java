package com.cinema.wallet;

import com.cinema.common.exception.ApiException;
import com.cinema.wallet.dto.envelope.ErrorEnvelope;
import com.cinema.wallet.dto.envelope.SuccessEnvelope;
import com.cinema.wallet.dto.request.AdminConfirmRequest;
import com.cinema.wallet.dto.request.TopUpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;

/**
 * Controller xử lý tất cả các HTTP Request đến module Wallet:
 * - GET  /wallet                     : Xem thông tin và số dư ví
 * - POST /wallet/top-up              : Tạo yêu cầu nạp tiền (PENDING)
 * - GET  /wallet/top-up/{id}         : Theo dõi kết quả nạp tiền
 * - GET  /wallet/transaction         : Lịch sử bút toán ví đã hoàn tất
 * - GET  /wallet/top-up/pending      : (ADMIN) Danh sách yêu cầu chờ duyệt
 * - POST /wallet/top-up/{id}/confirm : (ADMIN) Duyệt nạp tiền
 */
@WebServlet(urlPatterns = {"/wallet", "/wallet/*"})
public class WalletController extends HttpServlet {

    private final WalletService service = new WalletService();
    private final ObjectMapper json = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String traceId = resolveTraceId(req);
        try {
            long userId = getUserId(req);
            String role = getRole(req);
            String action = pathInfo(req);

            // 1. GET /wallet
            if ("/".equals(action) || action.isEmpty()) {
                var data = service.getMyWallet(userId);
                writeJson(resp, HttpServletResponse.SC_OK, SuccessEnvelope.of(data, traceId));
                return;
            }

            // 2. GET /wallet/transaction
            if ("/transaction".equals(action)) {
                String type = req.getParameter("type");
                String from = req.getParameter("from");
                String to = req.getParameter("to");
                int page = parseQueryInt(req.getParameter("page"), 0);
                int size = parseQueryInt(req.getParameter("size"), 20);

                var history = service.getTransactionHistory(userId, type, from, to, page, size);
                writeJson(resp, HttpServletResponse.SC_OK, SuccessEnvelope.of(history.getItems(), history.getMeta(), traceId));
                return;
            }

            // 3. GET /wallet/top-up/pending (Role ADMIN)
            if ("/top-up/pending".equals(action)) {
                requireAdmin(role);
                int page = parseQueryInt(req.getParameter("page"), 0);
                int size = parseQueryInt(req.getParameter("size"), 20);

                var pending = service.getPendingTopUps(page, size);
                writeJson(resp, HttpServletResponse.SC_OK, SuccessEnvelope.of(pending.getItems(), pending.getMeta(), traceId));
                return;
            }

            // 4. GET /wallet/top-up/{id}
            if (action.startsWith("/top-up/")) {
                String idStr = action.substring("/top-up/".length());
                long txId = parseLongId(idStr);
                var data = service.getTopUpStatus(userId, txId);
                writeJson(resp, HttpServletResponse.SC_OK, SuccessEnvelope.of(data, traceId));
                return;
            }

            throw WalletException.resourceNotFound("Endpoint không tồn tại: " + req.getRequestURI());

        } catch (Exception ex) {
            handleError(resp, ex, traceId);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String traceId = resolveTraceId(req);
        try {
            long userId = getUserId(req);
            String role = getRole(req);
            String action = pathInfo(req);

            // 1. POST /wallet/top-up: Tạo yêu cầu nạp tiền
            if ("/top-up".equals(action)) {
                String idempotencyKey = req.getHeader("Idempotency-Key");
                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                    idempotencyKey = req.getHeader("idempotencyKey");
                }

                TopUpRequest body = json.readValue(req.getInputStream(), TopUpRequest.class);
                var data = service.topUp(userId, body, idempotencyKey);

                writeJson(resp, HttpServletResponse.SC_CREATED, SuccessEnvelope.of(data, traceId));
                return;
            }

            // 2. POST /wallet/top-up/{id}/confirm: ADMIN duyệt nạp tiền
            if (action.startsWith("/top-up/") && action.endsWith("/confirm")) {
                requireAdmin(role);
                String idStr = action.substring("/top-up/".length(), action.length() - "/confirm".length());
                long txId = parseLongId(idStr);

                AdminConfirmRequest body = json.readValue(req.getInputStream(), AdminConfirmRequest.class);
                var data = service.confirmTopUp(txId, body);

                writeJson(resp, HttpServletResponse.SC_OK, SuccessEnvelope.of(data, traceId));
                return;
            }

            throw WalletException.resourceNotFound("Endpoint không tồn tại: " + req.getRequestURI());

        } catch (Exception ex) {
            handleError(resp, ex, traceId);
        }
    }

    // ==================== HELPER METHODS ====================

    private String pathInfo(HttpServletRequest req) {
        String p = req.getPathInfo();
        if (p == null) return "/";
        if (p.endsWith("/") && p.length() > 1) {
            return p.substring(0, p.length() - 1);
        }
        return p;
    }

    private long getUserId(HttpServletRequest req) {
        Object uid = req.getAttribute("userId");
        if (uid instanceof Long l) return l;
        if (uid instanceof Integer i) return i.longValue();
        throw ApiException.unauthorized("Chưa đăng nhập hoặc thiếu token xác thực");
    }

    private String getRole(HttpServletRequest req) {
        Object role = req.getAttribute("role");
        return role != null ? role.toString() : "USER";
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw WalletException.forbidden("Chỉ quản trị viên (ADMIN) mới có quyền thực hiện hành động này");
        }
    }

    private long parseLongId(String idStr) {
        try {
            return Long.parseLong(idStr.trim());
        } catch (Exception e) {
            throw WalletException.resourceNotFound("Mã định danh không hợp lệ: " + idStr);
        }
    }

    private int parseQueryInt(String val, int defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private String resolveTraceId(HttpServletRequest req) {
        String headerTrace = req.getHeader("X-Request-Id");
        if (headerTrace == null || headerTrace.isBlank()) {
            headerTrace = req.getHeader("traceId");
        }
        if (headerTrace != null && !headerTrace.isBlank()) {
            return headerTrace;
        }
        return "req-" + UUID.randomUUID();
    }

    private void writeJson(HttpServletResponse resp, int statusCode, Object body) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json;charset=UTF-8");
        json.writeValue(resp.getWriter(), body);
    }

    private void handleError(HttpServletResponse resp, Exception ex, String traceId) throws IOException {
        int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        ErrorEnvelope envelope;

        if (ex instanceof WalletException we) {
            status = we.getStatus();
            envelope = ErrorEnvelope.of(we.getCode(), we.getMessage(), we.getFieldErrors(), traceId);
        } else if (ex instanceof ApiException apiEx) {
            status = apiEx.getStatus();
            String code = switch (status) {
                case 400 -> "BAD_REQUEST";
                case 401 -> "UNAUTHORIZED";
                case 403 -> "FORBIDDEN";
                case 404 -> "RESOURCE_NOT_FOUND";
                case 409 -> "CONFLICT";
                default  -> "INTERNAL_ERROR";
            };
            envelope = ErrorEnvelope.of(code, apiEx.getMessage(), traceId);
        } else {
            ex.printStackTrace();
            envelope = ErrorEnvelope.of("INTERNAL_ERROR", "Lỗi server nội bộ: " + ex.getMessage(), traceId);
        }

        writeJson(resp, status, envelope);
    }
}
