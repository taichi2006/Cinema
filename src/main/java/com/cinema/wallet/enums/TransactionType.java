package com.cinema.wallet.enums;

public enum TransactionType {
    TOP_UP("CREDIT"), // nạp tiền
    PAYMENT("DEBIT"), // thanh toán
    REFUND("CREDIT"); // hoàn tiền

    private final String direction;

    TransactionType(String direction) {
        this.direction = direction;
    }

    public String getDirection() {
        return direction;
    }
}
