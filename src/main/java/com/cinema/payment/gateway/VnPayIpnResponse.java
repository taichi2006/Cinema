package com.cinema.payment.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VnPayIpnResponse(
        @JsonProperty("RspCode") String rspCode,
        @JsonProperty("Message") String message) {

    public static VnPayIpnResponse of(String rspCode, String message) {
        return new VnPayIpnResponse(rspCode, message);
    }
}
