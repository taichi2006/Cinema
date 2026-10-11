package com.cinema.product;

import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.exception.ApiException;

import java.util.Collections;
import java.util.List;

public class ProductService {

    private final ProductDAO productDAO = new ProductDAO();

    // Danh sách sản phẩm F&B kèm phân trang và lọc category, status
    public PageResponse<ProductResponse> getProducts(String category, String statusStr, int page, int size) {
        ProductStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = ProductStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest("Trạng thái sản phẩm không hợp lệ: " + statusStr);
            }
        }

        long total = productDAO.countTotal(category, status);
        List<ProductResponse> items = (total == 0 || (long) page * size >= total)
                ? Collections.emptyList()
                : productDAO.findWithPaging(category, status, Math.max(0, page), Math.max(1, size))
                        .stream().map(ProductResponse::new).toList();

        return PageResponse.of(items, new PageMeta(page, size, total));
    }
}
