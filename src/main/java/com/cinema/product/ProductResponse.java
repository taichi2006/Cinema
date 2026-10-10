package com.cinema.product;

import java.math.BigDecimal;

// DTO trả về thông tin Product
public class ProductResponse {

    private Long productId;
    private String productName;
    private String description;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private ProductStatus status;

    public ProductResponse() {
    }

    public ProductResponse(Product product) {
        if (product != null) {
            this.productId = product.getProductId();
            this.productName = product.getProductName();
            this.description = product.getDescription();
            this.price = product.getPrice();
            this.category = product.getCategory();
            this.imageUrl = product.getImageUrl();
            this.status = product.getStatus();
        }
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status;
    }
}
