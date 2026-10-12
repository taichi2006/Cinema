package com.cinema.wallet.controller;

import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import com.cinema.wallet.dto.request.TopUpRequest;
import com.cinema.wallet.service.WalletService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/wallet", "/wallet/*"})
public class WalletController extends BaseServlet {

    private final WalletService service = new WalletService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // GET /wallet (Xem số dư ví)
        if (segments.length == 0) {
            writeSuccess(resp, service.getMyWallet(userId));
            return;
        }

        // GET /wallet/transactions (Lịch sử giao dịch)
        if (segments.length == 1 && "transactions".equalsIgnoreCase(segments[0])) {
            String type = req.getParameter("type");
            String from = req.getParameter("from");
            String to = req.getParameter("to");
            int page = getIntParam(req, "page", 0);
            int size = getIntParam(req, "size", 20);

            writeSuccess(resp, service.getTransactionHistory(userId, type, from, to, page, size));
            return;
        }

        // GET /wallet/top-up/{id} (Trạng thái nạp tiền)
        if (segments.length == 2 && "top-up".equalsIgnoreCase(segments[0])) {
            long txId = parsePathId(segments[1], "Mã giao dịch");
            writeSuccess(resp, service.getTopUpStatus(userId, txId));
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = getAuthenticatedUserId(req);
        String[] segments = getPathSegments(req);

        // POST /wallet/top-up (Yêu cầu nạp tiền)
        if (segments.length == 1 && "top-up".equalsIgnoreCase(segments[0])) {
            TopUpRequest body = parseBody(req, TopUpRequest.class);
            resp.setStatus(HttpServletResponse.SC_CREATED);
            writeSuccess(resp, service.topUp(userId, body, req.getRemoteAddr()));
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}
