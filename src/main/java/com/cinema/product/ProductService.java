package com.cinema.product;

import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.exception.ApiException;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ProductService {

    private final ProductDAO productDAO;

    public ProductService() {
        this.productDAO = new ProductDAO();
    }

    public ProductService(ProductDAO productDAO) {
        this.productDAO = productDAO;
    }

    // Lấy danh sách sản phẩm F&B kèm phân trang và lọc category, status.
    public PageResponse<ProductResponse> getProducts(String category, String statusStr, int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100; // giới hạn an toàn

        ProductStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = ProductStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("Trạng thái sản phẩm không hợp lệ: " + statusStr);
            }
        }

        long totalElements = productDAO.countTotal(category, status);

        List<ProductResponse> items;
        if (totalElements == 0 || page * size >= totalElements) {
            items = Collections.emptyList();
        } else {
            items = productDAO.findWithPaging(category, status, page, size)
                    .stream()
                    .map(ProductResponse::new)
                    .collect(Collectors.toList());
        }

        PageMeta meta = new PageMeta(page, size, totalElements);

        return new PageResponse<>(items, meta);
    }
}
