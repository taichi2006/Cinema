package com.cinema.booking.dto;

import java.util.List;

public class CreateBookingRequest {
    private Integer showTimeId;
    private List<Integer> seatIds;
    private List<ProductItem> productItems;

    public static class ProductItem {
        private Integer productId;
        private Integer quantity;

        public ProductItem() {}

        public ProductItem(Integer productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public CreateBookingRequest() {}

    public CreateBookingRequest(Integer showTimeId, List<Integer> seatIds, List<ProductItem> productItems) {
        this.showTimeId = showTimeId;
        this.seatIds = seatIds;
        this.productItems = productItems;
    }

    public Integer getShowTimeId() { return showTimeId; }
    public void setShowTimeId(Integer showTimeId) { this.showTimeId = showTimeId; }

    public List<Integer> getSeatIds() { return seatIds; }
    public void setSeatIds(List<Integer> seatIds) { this.seatIds = seatIds; }

    public List<ProductItem> getProductItems() { return productItems; }
    public void setProductItems(List<ProductItem> productItems) { this.productItems = productItems; }
}
