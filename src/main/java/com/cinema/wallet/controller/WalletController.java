package com.cinema.wallet;

import com.cinema.common.dto.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.wallet.dto.request.TopUpRequest;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * - GET  /wallet             : Xem thông tin và số dư ví
 * - POST /wallet/top-up      : NÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â¡p tiÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€šÃ‚Ân vÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â o vÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â­ 
 * - GET  /wallet/top-up/{id} : Theo dÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Âµi kÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â¿t quÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â£ nÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚ÂºÃƒâ€šÃ‚Â¡p tiÃƒÆ’Ã‚Â¡Ãƒâ€šÃ‚Â»Ãƒâ€šÃ‚Ân
 * - GET  /wallet/transaction : Lịch sử bút toán ví đã hoàn tất
 */
@WebServlet(urlPatterns = {"/wallet", "/wallet/*"})
public class WalletController extends BaseServlet {

    private final WalletService service = new WalletService();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            long userId = getAuthenticatedUserId(req);
            String[] segments = getPathSegments(req);

            // 1. GET /wallet
            if (segments.length == 0) {
                var data = service.getMyWallet(userId);
                writeSuccess(resp, data);
                return;
            }

            // 2. GET /wallet/transaction (Phân trang danh sách giao dịch)
            if (segments.length == 1 && "transaction".equals(segments[0])) {
                String type = req.getParameter("type");
                String from = req.getParameter("from");
                String to = req.getParameter("to");
                int page = getIntParam(req, "page", 0);
                int size = getIntParam(req, "size", 20);

                var history = service.getTransactionHistory(userId, type, from, to, page, size);
                writeSuccess(resp, history);
                return;
            }

            // 3. GET /wallet/top-up/{id}
            if (segments.length == 2 && "top-up".equals(segments[0])) {
                long txId = parsePathId(segments[1], "Mã giao dịch");
                var data = service.getTopUpStatus(userId, txId);
                writeSuccess(resp, data);
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            long userId = getAuthenticatedUserId(req);
            String[] segments = getPathSegments(req);

            // 1. POST /wallet/top-up: Nạp tiền vào ví
            if (segments.length == 1 && "top-up".equals(segments[0])) {
                TopUpRequest body = parseBody(req, TopUpRequest.class);
                var data = service.topUp(userId, body);
                resp.setStatus(HttpServletResponse.SC_CREATED);
                writeSuccess(resp, data);
                return;
            }

            throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}

