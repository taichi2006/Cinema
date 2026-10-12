package com.cinema.payment.controller;

import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import com.cinema.payment.service.PaymentService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(urlPatterns = {"/payments", "/payments/*"})
public class PaymentController extends BaseServlet {

    private final PaymentService service = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] segments = getPathSegments(req);

        // VNPAY IPN là request server-to-server, xác thực bằng checksum thay vì JWT.
        if (segments.length == 1 && "webhook".equalsIgnoreCase(segments[0])) {
            resp.setStatus(HttpServletResponse.SC_OK);
            sendJson(resp, service.handleVnPayIpn(req.getParameterMap()));
            return;
        }

        // VNPAY Return URL: trình duyệt redirect về sau khi thanh toán
        if (segments.length == 1 && "vnpay-return".equalsIgnoreCase(segments[0])) {
            resp.setStatus(HttpServletResponse.SC_OK);
            writeSuccess(resp, service.handleVnPayIpn(req.getParameterMap()));
            return;
        }

        long userId = getAuthenticatedUserId(req);

        if (segments.length == 1) {
            long paymentId = parsePathId(segments[0], "Mã giao dịch thanh toán");
            writeSuccess(resp, service.getPaymentDetail(userId, paymentId));
            return;
        }

        throw ApiException.notFound("Endpoint không tồn tại: " + req.getRequestURI());
    }
}
