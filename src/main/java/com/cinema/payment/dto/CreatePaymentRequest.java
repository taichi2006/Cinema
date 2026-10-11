package com.cinema.payment.dto;

public class CreatePaymentRequest {
    private String method;

    public CreatePaymentRequest() {
    }

    public CreatePaymentRequest(String method) {
        this.method = method;
    }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
}
