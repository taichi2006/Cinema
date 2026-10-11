package com.cinema.payment.gateway;

import java.math.BigDecimal;

public record VnPayIpnResult(
        boolean validSignature,
        Long paymentId,
        BigDecimal amount,
        boolean paymentSuccessful,
        String transactionNo,
        String responseCode,
        String transactionStatus) {

    public static VnPayIpnResult invalidSignature() {
        return new VnPayIpnResult(false, null, null, false, null, null, null);
    }
}
