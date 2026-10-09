package com.cinema.wallet.dto.response;

/**
 * Response schema cho kết quả tạo/theo dõi yêu cầu nạp tiền (POST /wallet/top-up, GET /wallet/top-up/{id}).
 */
public class TopUpResponse {

    private String id;
    private Long amount;
    private String method;
    private String status;
    private String checkoutUrl;
    private String expiresAt;
    private String createdAt;
    private String completedAt;
    private String failureCode;

    public TopUpResponse() {}

    public TopUpResponse(String id, Long amount, String method, String status,
                         String checkoutUrl, String expiresAt, String createdAt, String completedAt, String failureCode) {
        this.id = id;
        this.amount = amount;
        this.method = method;
        this.status = status;
        this.checkoutUrl = checkoutUrl;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.failureCode = failureCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCheckoutUrl() {
        return checkoutUrl;
    }

    public void setCheckoutUrl(String checkoutUrl) {
        this.checkoutUrl = checkoutUrl;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(String completedAt) {
        this.completedAt = completedAt;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }
}
