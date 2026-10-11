package com.cinema.payment;

import com.cinema.common.config.ObjectMapperConfig;
import com.cinema.common.dto.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.payment.dto.PaymentResponse;
import com.cinema.payment.dto.CreatePaymentRequest;
import com.cinema.payment.entity.Payment;
import com.cinema.payment.enums.PaymentMethod;
import com.cinema.payment.enums.PaymentStatus;
import com.cinema.payment.enums.PaymentType;
import com.cinema.payment.gateway.VnPayConfig;
import com.cinema.payment.gateway.VnPayGateway;
import com.cinema.payment.gateway.VnPayIpnResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentModuleTest {

    @Test
    @DisplayName("Payment entity và DTO ánh xạ đúng dữ liệu bảng payments")
    void mapsPaymentToResponse() throws Exception {
        Payment payment = payment(701L, 101L, PaymentMethod.VNPAY, PaymentStatus.PENDING);
        payment.setExpiredAt(LocalDateTime.of(2026, 10, 11, 15, 10));

        PaymentResponse response = new PaymentResponse(payment);
        String json = ObjectMapperConfig.getInstance().writeValueAsString(ApiResponse.ok(response));

        assertEquals(701L, response.getPaymentId());
        assertEquals(101L, response.getBookingId());
        assertEquals(PaymentMethod.VNPAY, response.getMethod());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertTrue(json.contains("\"paymentId\":701"));
        assertTrue(json.contains("\"method\":\"VNPAY\""));
        assertTrue(json.contains("\"status\":\"PENDING\""));
    }

    @Test
    @DisplayName("Lấy danh sách payment khi user sở hữu booking")
    void returnsPaymentsForBookingOwner() {
        PaymentDAO fakeDAO = new PaymentDAO() {
            @Override
            public Optional<Long> findBookingOwnerId(long bookingId) {
                return Optional.of(10L);
            }

            @Override
            public List<Payment> findByBookingId(long bookingId) {
                return List.of(
                        payment(702L, bookingId, PaymentMethod.WALLET, PaymentStatus.SUCCESSFUL),
                        payment(701L, bookingId, PaymentMethod.VNPAY, PaymentStatus.FAILED)
                );
            }
        };

        List<PaymentResponse> result = new PaymentService(fakeDAO).getPaymentsByBooking(10L, 101L);

        assertEquals(2, result.size());
        assertEquals(702L, result.get(0).getPaymentId());
        assertEquals(PaymentStatus.SUCCESSFUL, result.get(0).getStatus());
    }

    @Test
    @DisplayName("Không cho user khác xem payment")
    void rejectsPaymentReadForDifferentOwner() {
        Payment payment = payment(701L, 101L, PaymentMethod.VNPAY, PaymentStatus.PENDING);
        PaymentDAO fakeDAO = new PaymentDAO() {
            @Override
            public Optional<Payment> findById(long paymentId) {
                return Optional.of(payment);
            }

            @Override
            public Optional<Long> findBookingOwnerId(long bookingId) {
                return Optional.of(99L);
            }
        };

        ApiException exception = assertThrows(ApiException.class,
                () -> new PaymentService(fakeDAO).getPaymentDetail(10L, 701L));

        assertEquals(403, exception.getStatus());
    }

    @Test
    @DisplayName("Trả 404 khi payment không tồn tại")
    void rejectsUnknownPayment() {
        PaymentDAO fakeDAO = new PaymentDAO() {
            @Override
            public Optional<Payment> findById(long paymentId) {
                return Optional.empty();
            }
        };

        ApiException exception = assertThrows(ApiException.class,
                () -> new PaymentService(fakeDAO).getPaymentDetail(10L, 999L));

        assertEquals(404, exception.getStatus());
    }

    @Test
    @DisplayName("Chỉ chấp nhận phương thức WALLET hoặc VNPAY")
    void acceptsOnlySupportedMethodsForCreatePayment() {
        assertEquals(PaymentMethod.WALLET,
                PaymentService.requireSupportedMethod(new CreatePaymentRequest("wallet")));
        assertEquals(PaymentMethod.VNPAY,
                PaymentService.requireSupportedMethod(new CreatePaymentRequest("VNPAY")));

        ApiException unsupported = assertThrows(ApiException.class,
                () -> PaymentService.requireSupportedMethod(new CreatePaymentRequest("MOMO")));
        assertEquals(422, unsupported.getStatus());

        ApiException invalid = assertThrows(ApiException.class,
                () -> PaymentService.requireSupportedMethod(new CreatePaymentRequest("bank-transfer")));
        assertEquals(400, invalid.getStatus());
    }

    @Test
    @DisplayName("VNPAY tạo URL và xác minh IPN bằng cùng checksum")
    void createsAndVerifiesVnPaySignature() {
        VnPayGateway gateway = new VnPayGateway(new VnPayConfig(
                "TESTCODE", "test-secret", "https://pay.example/vpcpay.html",
                "https://cinema.example/payment/result", ZoneId.of("Asia/Ho_Chi_Minh")));
        Payment payment = payment(701L, 101L, PaymentMethod.VNPAY, PaymentStatus.PENDING);
        payment.setExpiredAt(LocalDateTime.of(2026, 10, 11, 15, 10));

        String url = gateway.createPaymentUrl(payment, "127.0.0.1",
                LocalDateTime.of(2026, 10, 11, 15, 0));
        assertTrue(url.startsWith("https://pay.example/vpcpay.html?"));
        assertTrue(url.contains("vnp_TxnRef=701"));
        assertTrue(url.contains("vnp_Amount=31500000"));
        assertTrue(url.contains("vnp_SecureHash="));

        Map<String, String> signedParameters = new HashMap<>();
        signedParameters.put("vnp_TmnCode", "TESTCODE");
        signedParameters.put("vnp_TxnRef", "701");
        signedParameters.put("vnp_Amount", "31500000");
        signedParameters.put("vnp_ResponseCode", "00");
        signedParameters.put("vnp_TransactionStatus", "00");
        signedParameters.put("vnp_TransactionNo", "14592038");
        String signature = gateway.signParameters(signedParameters);

        Map<String, String[]> callback = new HashMap<>();
        signedParameters.forEach((key, value) -> callback.put(key, new String[]{value}));
        callback.put("vnp_SecureHash", new String[]{signature});

        VnPayIpnResult result = gateway.verifyIpn(callback);
        assertTrue(result.validSignature());
        assertTrue(result.paymentSuccessful());
        assertEquals(701L, result.paymentId());
        assertEquals(0, BigDecimal.valueOf(315000).compareTo(result.amount()));
        assertEquals("14592038", result.transactionNo());

        callback.put("vnp_SecureHash", new String[]{"invalid"});
        assertTrue(!gateway.verifyIpn(callback).validSignature());
    }

    private Payment payment(Long id, Long bookingId, PaymentMethod method, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setBookingId(bookingId);
        payment.setAmount(BigDecimal.valueOf(315000));
        payment.setMethod(method);
        payment.setStatus(status);
        payment.setPaymentType(PaymentType.BOOKING_PAYMENT);
        return payment;
    }
}
