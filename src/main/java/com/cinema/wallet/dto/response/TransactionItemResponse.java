package com.cinema.wallet.dto.response;

/**
 * Response schema cho từng mục bút toán lịch sử giao dịch ví (GET /wallet/transaction).
 */
public class TransactionItemResponse {

    private String id;
    private String type;
    private String status;
    private Long amount;
    private String description;
    private String createdAt;

    public TransactionItemResponse() {}

    public TransactionItemResponse(String id, String type, String status, Long amount, String description, String createdAt) {
        this.id = id;
        this.type = type;
        this.status = status;
        this.amount = amount;
        this.description = description;
        this.createdAt = createdAt;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
