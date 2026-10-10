package com.cinema.product;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.dto.CommonDTO.PageMeta;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProductModuleTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("Product entity mapping đúng dữ liệu các thuộc tính")
    void testProductEntity() {
        Product product = new Product("Bắp rang bơ phô mai", BigDecimal.valueOf(45000), "POPCORN", ProductStatus.AVAILABLE);
        product.setProductId(1L);
        product.setDescription("Bắp vị phô mai giòn rụm");
        product.setImageUrl("https://example.com/popcorn.jpg");

        assertEquals(1L, product.getProductId());
        assertEquals("Bắp rang bơ phô mai", product.getProductName());
        assertEquals("Bắp vị phô mai giòn rụm", product.getDescription());
        assertEquals(BigDecimal.valueOf(45000), product.getPrice());
        assertEquals("POPCORN", product.getCategory());
        assertEquals("https://example.com/popcorn.jpg", product.getImageUrl());
        assertEquals(ProductStatus.AVAILABLE, product.getStatus());
    }

    @Test
    @DisplayName("ProductResponse DTO mapping chính xác từ Entity")
    void testProductResponseMapping() {
        Product product = new Product("Combo 1", BigDecimal.valueOf(85000), "COMBO", ProductStatus.AVAILABLE);
        product.setProductId(10L);
        product.setDescription("Bao gồm 1 bắp và 2 nước ngọt");
        product.setImageUrl("https://example.com/combo1.jpg");

        ProductResponse response = new ProductResponse(product);

        assertEquals(10L, response.getProductId());
        assertEquals("Combo 1", response.getProductName());
        assertEquals("Bao gồm 1 bắp và 2 nước ngọt", response.getDescription());
        assertEquals(BigDecimal.valueOf(85000), response.getPrice());
        assertEquals("COMBO", response.getCategory());
        assertEquals("https://example.com/combo1.jpg", response.getImageUrl());
        assertEquals(ProductStatus.AVAILABLE, response.getStatus());
    }

    @Test
    @DisplayName("ProductResponse serialize ra JSON đúng cấu trúc Swagger")
    void testProductResponseSerialization() throws Exception {
        Product product = new Product("Coca Cola lớn", BigDecimal.valueOf(30000), "DRINK", ProductStatus.AVAILABLE);
        product.setProductId(5L);

        ProductResponse response = new ProductResponse(product);
        PageMeta meta = new PageMeta(0, 20, 1L, 1);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", List.of(response));
        data.put("meta", meta);

        ApiResponse<Map<String, Object>> apiResponse = ApiResponse.ok(data);
        String jsonStr = json.writeValueAsString(apiResponse);

        assertTrue(jsonStr.contains("\"productId\":5"));
        assertTrue(jsonStr.contains("\"productName\":\"Coca Cola lớn\""));
        assertTrue(jsonStr.contains("\"status\":\"AVAILABLE\""));
        assertTrue(jsonStr.contains("\"success\":true"));
        assertTrue(jsonStr.contains("\"totalElements\":1"));
    }

    @Test
    @DisplayName("ProductException ném đúng status và error message")
    void testProductException() {
        ProductException ex = ProductException.badRequest("Trạng thái không hợp lệ");
        assertEquals(400, ex.getStatus());
        assertEquals("Trạng thái không hợp lệ", ex.getMessage());

        ProductException notFoundEx = ProductException.notFound("Không tìm thấy sản phẩm");
        assertEquals(404, notFoundEx.getStatus());
        assertEquals("Không tìm thấy sản phẩm", notFoundEx.getMessage());
    }

    @Test
    @DisplayName("ProductService ném lỗi 400 khi status chuỗi không hợp lệ")
    void testServiceInvalidStatus() {
        ProductDAO fakeDao = new ProductDAO() {
            @Override
            public long countTotal(String category, ProductStatus status) {
                return 0L;
            }

            @Override
            public List<Product> findWithPaging(String category, ProductStatus status, int page, int size) {
                return Collections.emptyList();
            }
        };

        ProductService service = new ProductService(fakeDao);
        ProductException ex = assertThrows(ProductException.class, () ->
                service.getProducts(null, "INVALID_STATUS_VALUE", 0, 10));

        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("Trạng thái sản phẩm không hợp lệ"));
    }

    @Test
    @DisplayName("ProductService chuẩn hóa phân trang âm và tính toán totalPages chuẩn")
    void testServicePaginationLogic() {
        ProductDAO fakeDao = new ProductDAO() {
            @Override
            public long countTotal(String category, ProductStatus status) {
                return 45L;
            }

            @Override
            public List<Product> findWithPaging(String category, ProductStatus status, int page, int size) {
                Product p = new Product("Snack", BigDecimal.valueOf(20000), "SNACK", ProductStatus.AVAILABLE);
                p.setProductId(1L);
                return List.of(p);
            }
        };

        ProductService service = new ProductService(fakeDao);
        Map<String, Object> result = service.getProducts(null, null, -1, -5);

        assertNotNull(result);
        PageMeta meta = (PageMeta) result.get("meta");
        assertEquals(0, meta.getPage());
        assertEquals(20, meta.getSize()); // mặc định size chuẩn hóa về 20
        assertEquals(45L, meta.getTotalElements());
        assertEquals(3, meta.getTotalPages()); // 45 / 20 = 2.25 -> 3 trang
    }
}
