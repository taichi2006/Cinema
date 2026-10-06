package com.cinema.wallet;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.common.exception.ErrorHandler;
import com.cinema.wallet.dto.request.TopUpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * - GET  /wallet             : Xem thông tin và số dư ví
 * - POST /wallet/top-up      : Nạp tiền vào ví 
 * - GET  /wallet/top-up/{id} : Theo dõi kết quả nạp tiền
 * - GET  /wallet/transaction : Lịch sử bút toán ví đã hoàn tất
 */
@WebServlet(urlPatterns = {"/wallet", "/wallet/*"})
public class WalletController extends HttpServlet {

    private final WalletService service = new WalletService();
    private final ObjectMapper json = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long userId = getUserId(req);
            String action = pathInfo(req);

            // 1. GET /wallet
            if ("/".equals(action) || action.isEmpty()) {
                var data = service.getMyWallet(userId);
                writeJson(resp, HttpServletResponse.SC_OK, ApiResponse.ok(data));
                return;
            }

            // 2. GET /wallet/transaction (Phân trang danh sách giao dịch)
            if ("/transaction".equals(action)) {
                String type = req.getParameter("type");
                String from = req.getParameter("from");
                String to = req.getParameter("to");
                int page = parseQueryInt(req.getParameter("page"), 0);
                int size = parseQueryInt(req.getParameter("size"), 20);

                var history = service.getTransactionHistory(userId, type, from, to, page, size);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("success", true);
                result.put("data", history.getItems());
                result.put("meta", history.getMeta());

                writeJson(resp, HttpServletResponse.SC_OK, result);
                return;
            }

            // 3. GET /wallet/top-up/{id}
            if (action.startsWith("/top-up/")) {
                String idStr = action.substring("/top-up/".length());
                long txId = parseLongId(idStr);
                var data = service.getTopUpStatus(userId, txId);
                writeJson(resp, HttpServletResponse.SC_OK, ApiResponse.ok(data));
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());

        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            long userId = getUserId(req);
            String action = pathInfo(req);

            // 1. POST /wallet/top-up: Nạp tiền vào ví
            if ("/top-up".equals(action)) {
                String idempotencyKey = req.getHeader("Idempotency-Key");
                if (idempotencyKey == null || idempotencyKey.isBlank()) {
                    idempotencyKey = req.getHeader("idempotencyKey");
                }

                TopUpRequest body = json.readValue(req.getInputStream(), TopUpRequest.class);
                var data = service.topUp(userId, body, idempotencyKey);

                writeJson(resp, HttpServletResponse.SC_CREATED, ApiResponse.ok(data));
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());

        } catch (Exception ex) {
            ErrorHandler.handle(resp, ex);
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

    private long parseLongId(String idStr) {
        try {
            return Long.parseLong(idStr.trim());
        } catch (Exception e) {
            throw ApiException.notFound("Mã định danh không hợp lệ: " + idStr);
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

    private void writeJson(HttpServletResponse resp, int statusCode, Object body) throws IOException {
        resp.setStatus(statusCode);
        resp.setContentType("application/json;charset=UTF-8");
        json.writeValue(resp.getWriter(), body);
    }
}

