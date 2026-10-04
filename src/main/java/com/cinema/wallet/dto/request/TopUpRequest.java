package com.cinema.wallet.dto.request;

/**
 * Request body cho API nạp tiền (POST /wallet/top-up).
 */
public class TopUpRequest {

    private Long amount;
    private String method;

    public TopUpRequest() {}

    public TopUpRequest(Long amount, String method) {
        this.amount = amount;
        this.method = method;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}

