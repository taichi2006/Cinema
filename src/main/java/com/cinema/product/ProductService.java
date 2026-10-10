package com.cinema.product;

import com.cinema.common.dto.CommonDTO.PageMeta;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    public Map<String, Object> getProducts(String category, String statusStr, int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        if (size > 100) size = 100; // giới hạn an toàn

        ProductStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = ProductStatus.valueOf(statusStr.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw ProductException.badRequest("Trạng thái sản phẩm không hợp lệ: " + statusStr);
            }
        }

        long totalElements = productDAO.countTotal(category, status);
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / size);

        List<ProductResponse> items;
        if (totalElements == 0 || page >= totalPages) {
            items = Collections.emptyList();
        } else {
            items = productDAO.findWithPaging(category, status, page, size)
                    .stream()
                    .map(ProductResponse::new)
                    .collect(Collectors.toList());
        }

        PageMeta meta = new PageMeta(page, size, totalElements, totalPages);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("meta", meta);
        return result;
    }
}
