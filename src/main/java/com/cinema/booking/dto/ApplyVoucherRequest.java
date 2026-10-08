package com.cinema.booking.dto;

public class ApplyVoucherRequest {
    private String code;

    public ApplyVoucherRequest() {}

    public ApplyVoucherRequest(String code) {
        this.code = code;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
