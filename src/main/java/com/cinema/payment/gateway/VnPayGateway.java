package com.cinema.payment.gateway;

import com.cinema.common.exception.ApiException;
import com.cinema.payment.entity.Payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

public class VnPayGateway {
    private static final DateTimeFormatter VNPAY_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final Supplier<VnPayConfig> configSupplier;

    public VnPayGateway() {
        this(VnPayConfig::fromEnvironment);
    }

    public VnPayGateway(VnPayConfig config) {
        this(() -> config);
    }

    private VnPayGateway(Supplier<VnPayConfig> configSupplier) {
        this.configSupplier = configSupplier;
    }

    public String createPaymentUrl(Payment payment, String clientIp, LocalDateTime createdAt) {
        if (payment == null || payment.getId() == null || payment.getBookingId() == null
                || payment.getAmount() == null || payment.getExpiredAt() == null) {
            throw ApiException.internal("Thiếu dữ liệu để tạo giao dịch VNPAY");
        }

        VnPayConfig config = configSupplier.get();
        TreeMap<String, String> parameters = new TreeMap<>();
        parameters.put("vnp_Version", "2.1.0");
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", config.tmnCode());
        parameters.put("vnp_Amount", toVnPayAmount(payment.getAmount()));
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", payment.getId().toString());
        parameters.put("vnp_OrderInfo", "Thanh toan booking #" + payment.getBookingId());
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_ReturnUrl", config.returnUrl());
        parameters.put("vnp_IpAddr", normalizeIp(clientIp));
        parameters.put("vnp_CreateDate", VNPAY_TIME.format(createdAt.atZone(config.zoneId())));
        parameters.put("vnp_ExpireDate", VNPAY_TIME.format(payment.getExpiredAt().atZone(config.zoneId())));

        String query = canonicalQuery(parameters);
        String secureHash = hmacSha512(config.hashSecret(), query);
        return config.paymentUrl() + (config.paymentUrl().contains("?") ? "&" : "?")
                + query + "&vnp_SecureHash=" + secureHash;
    }

    public VnPayIpnResult verifyIpn(Map<String, String[]> rawParameters) {
        VnPayConfig config = configSupplier.get();
        String receivedHash = firstValue(rawParameters, "vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            return VnPayIpnResult.invalidSignature();
        }

        TreeMap<String, String> parameters = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : rawParameters.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith("vnp_")
                    || "vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) {
                continue;
            }
            String value = firstValue(entry.getValue());
            if (value != null) {
                parameters.put(key, value);
            }
        }

        String expectedHash = hmacSha512(config.hashSecret(), canonicalQuery(parameters));
        if (!MessageDigest.isEqual(expectedHash.getBytes(StandardCharsets.US_ASCII),
                receivedHash.getBytes(StandardCharsets.US_ASCII))) {
            return VnPayIpnResult.invalidSignature();
        }

        try {
            long paymentId = Long.parseLong(required(parameters, "vnp_TxnRef"));
            BigDecimal amount = new BigDecimal(required(parameters, "vnp_Amount")).movePointLeft(2);
            String responseCode = required(parameters, "vnp_ResponseCode");
            String transactionStatus = required(parameters, "vnp_TransactionStatus");
            String transactionNo = parameters.get("vnp_TransactionNo");
            boolean successful = "00".equals(responseCode) && "00".equals(transactionStatus);
            return new VnPayIpnResult(true, paymentId, amount, successful,
                    transactionNo, responseCode, transactionStatus);
        } catch (IllegalArgumentException exception) {
            return new VnPayIpnResult(true, null, null, false, null, null, null);
        }
    }

    public String signParameters(Map<String, String> parameters) {
        return hmacSha512(configSupplier.get().hashSecret(), canonicalQuery(new TreeMap<>(parameters)));
    }

    private static String toVnPayAmount(BigDecimal amount) {
        try {
            return amount.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).toPlainString();
        } catch (ArithmeticException exception) {
            throw ApiException.unprocessable("Số tiền thanh toán phải có độ chính xác đến VND");
        }
    }

    private static String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp.trim();
    }

    private static String required(Map<String, String> parameters, String name) {
        String value = parameters.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Thiếu " + name);
        }
        return value;
    }

    private static String firstValue(Map<String, String[]> parameters, String name) {
        return parameters == null ? null : firstValue(parameters.get(name));
    }

    private static String firstValue(String[] values) {
        return values == null || values.length == 0 ? null : values[0];
    }

    private static String canonicalQuery(Map<String, String> parameters) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            if (builder.length() > 0) {
                builder.append('&');
            }
            builder.append(urlEncode(entry.getKey())).append('=').append(urlEncode(entry.getValue()));
        }
        return builder.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw ApiException.internal("Không thể tạo chữ ký VNPAY");
        }
    }
}
