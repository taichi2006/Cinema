package com.cinema.payment;

import com.cinema.common.exception.ApiException;
import com.cinema.common.web.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "PaymentController", urlPatterns = {"/payments", "/payments/*"})
public class PaymentController extends BaseServlet {

    private final PaymentService service;

    public PaymentController() {
        this(new PaymentService());
    }

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String[] segments = getPathSegments(req);

        // VNPAY IPN là request server-to-server, xác thực bằng checksum thay vì JWT.
        if (segments.length == 1 && "webhook".equalsIgnoreCase(segments[0])) {
            resp.setStatus(HttpServletResponse.SC_OK);
            sendJson(resp, service.handleVnPayIpn(req.getParameterMap()));
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
