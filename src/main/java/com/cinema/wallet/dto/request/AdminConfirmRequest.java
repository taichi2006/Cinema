package com.cinema.wallet.dto.request;

/**
 * Request body cho API Admin duyệt nạp tiền (POST /wallet/top-up/{id}/confirm).
 */
public class AdminConfirmRequest {

    private Boolean approve;
    private String note;

    public AdminConfirmRequest() {}

    public AdminConfirmRequest(Boolean approve, String note) {
        this.approve = approve;
        this.note = note;
    }

    public Boolean getApprove() {
        return approve;
    }

    public void setApprove(Boolean approve) {
        this.approve = approve;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
