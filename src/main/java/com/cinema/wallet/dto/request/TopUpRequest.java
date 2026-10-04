package com.cinema.wallet.dto.request;

import java.math.BigDecimal;

/**
 * Request body cho API nạp tiền (POST /wallet/top-up).
 */
public class TopUpRequest {

    private BigDecimal amount;
    private String method;

    public TopUpRequest() {}

    public TopUpRequest(BigDecimal amount, String method) {
        this.amount = amount;
        this.method = method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
