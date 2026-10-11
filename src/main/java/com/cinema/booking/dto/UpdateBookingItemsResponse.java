package com.cinema.booking.dto;

import java.math.BigDecimal;
import java.util.List;

public class UpdateBookingItemsResponse {
    private Integer bookingId;
    private BigDecimal totalAmount;
    private List<ProductItemResponse> products;

    public static class ProductItemResponse {
        private Integer productId;
        private Integer quantity;
        private BigDecimal price;

        public ProductItemResponse() {}

        public ProductItemResponse(Integer productId, Integer quantity, BigDecimal price) {
            this.productId = productId;
            this.quantity = quantity;
            this.price = price;
        }

        public Integer getProductId() { return productId; }
        public void setProductId(Integer productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    public UpdateBookingItemsResponse() {}

    public UpdateBookingItemsResponse(Integer bookingId, BigDecimal totalAmount, List<ProductItemResponse> products) {
        this.bookingId = bookingId;
        this.totalAmount = totalAmount;
        this.products = products;
    }

    public Integer getBookingId() { return bookingId; }
    public void setBookingId(Integer bookingId) { this.bookingId = bookingId; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public List<ProductItemResponse> getProducts() { return products; }
    public void setProducts(List<ProductItemResponse> products) { this.products = products; }
}
