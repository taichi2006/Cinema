package com.cinema.booking.dto;

import java.util.List;

public class UpdateBookingItemsRequest {
    private List<Item> items;

    public static class Item {
        private Integer productId;
        private Integer quantity;

        public Item() {}

        public Item(Integer productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public UpdateBookingItemsRequest() {}

    public UpdateBookingItemsRequest(List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}
