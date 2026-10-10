# 📋 Kế hoạch triển khai Module `product` (F&B - Sản phẩm bắp nước)

> **Tài liệu tham chiếu chuẩn**:
> - [cinema sql.txt](file:///d:/IT1_UTE/HK5_26_27/lap_trinh_web/project/cinema_system/cinema%20sql.txt) (Đặc tả CSDL chuẩn)
> - [swagger_cinema.yaml](file:///d:/IT1_UTE/HK5_26_27/lap_trinh_web/project/cinema_system/swagger_cinema.yaml) (Đặc tả API)
> - [PROJECT_STRUCTURE.md](file:///d:/IT1_UTE/HK5_26_27/lap_trinh_web/project/cinema_system/PROJECT_STRUCTURE.md) (Kiến trúc dự án)

---

## 1. Phân tích CSDL & API Specification

### 1.1 Bảng `products` trong `cinema sql.txt`
```sql
CREATE TABLE products (
    product_id    SERIAL PRIMARY KEY,
    product_name  VARCHAR(255) NOT NULL,
    description   TEXT,
    price         DECIMAL(10,2) NOT NULL,
    category      VARCHAR(100),
    image_url     VARCHAR(500),
    status        VARCHAR(20) DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE','OUT_OF_STOCK','DISCONTINUED'))
);
```

**Chi tiết các thuộc tính**:
- `product_id` (`INT`, Khóa chính sinh tự động `SERIAL`).
- `product_name` (`VARCHAR(255) NOT NULL`).
- `description` (`TEXT`, có thể `NULL`).
- `price` (`DECIMAL(10,2) NOT NULL`, giá bán của sản phẩm, tương ứng kiểu `BigDecimal` trong Java).
- `category` (`VARCHAR(100)`, danh mục sản phẩm như: `BẮP`, `NƯỚC`, `COMBO`,...).
- `image_url` (`VARCHAR(500)`, đường dẫn hình ảnh đại diện).
- `status` (`VARCHAR(20) CHECK (status IN ('AVAILABLE','OUT_OF_STOCK','DISCONTINUED'))`).

---

### 1.2 Endpoint API theo Swagger (`swagger_cinema.yaml`)
Module `Product` chỉ có **duy nhất 1 endpoint public**:

* **Endpoint**: `GET /products`
* **Xác thực**: Public (`security: []`, không yêu cầu Bearer Token).
* **Query Parameters**:
  - `category` (string, optional): Lọc theo danh mục.
  - `status` (string, optional): Lọc theo trạng thái (`AVAILABLE`, `OUT_OF_STOCK`, `DISCONTINUED`). Mặc định nếu không truyền có thể lấy tất cả hoặc lấy sản phẩm đang kinh doanh (`AVAILABLE`).
  - `page` (integer, default: 0): Số thứ tự trang (0-indexed).
  - `size` (integer, default: 20): Số bản ghi trên mỗi trang.
* **Response**: `200 OK`
  ```json
  {
    "success": true,
    "data": {
      "items": [
        {
          "productId": 1,
          "productName": "Bắp rang bơ phô mai",
          "description": "Bắp vị phô mai giòn rụm",
          "price": 45000,
          "category": "POPCORN",
          "imageUrl": "https://...",
          "status": "AVAILABLE"
        }
      ],
      "meta": {
        "page": 0,
        "size": 20,
        "totalElements": 1,
        "totalPages": 1
      }
    }
  }
  ```

---

## 2. Cấu trúc Thư mục Module `product`

Vì module `product` có nghiệp vụ gọn gàng (chỉ 1 API lấy danh sách sản phẩm) và mỗi thành phần chỉ có đúng 1 file, toàn bộ mã nguồn Java sẽ được đặt trực tiếp dưới package gốc `com.cinema.product` (tương tự như cách bố trí của module `movie`), không phân chia thêm các thư mục con 1 file không cần thiết:

```
src/main/java/com/cinema/product/
├── Product.java                       ← Entity JPA ánh xạ bảng cinema.products
├── ProductStatus.java                 ← Enum trạng thái: AVAILABLE, OUT_OF_STOCK, DISCONTINUED
├── ProductResponse.java               ← DTO trả về thông tin sản phẩm theo Swagger
├── ProductDAO.java                    ← Truy vấn DB qua JPA EntityManager (JPQL)
├── ProductService.java                ← Xử lý logic lọc, phân trang, mapping DTO
├── ProductController.java             ← Servlet ánh xạ GET /products
├── ProductException.java              ← Ngoại lệ nghiệp vụ riêng cho Product
├── docs/
│   ├── PLAN.md                        ← Kế hoạch thiết kế & triển khai này
│   └── POSTMAN_GUIDE.md               ← Hướng dẫn test Postman
└── test/
    └── product_postman.json           ← Postman Collection kiểm thử
```

---

## 3. Thiết kế Chi tiết Các Lớp Java

### 3.1 Enum `ProductStatus`
```java
package com.cinema.product;

public enum ProductStatus {
    AVAILABLE,
    OUT_OF_STOCK,
    DISCONTINUED
}
```

### 3.2 Entity `Product`
```java
package com.cinema.product;

@Entity
@Table(name = "products", schema = "cinema")
public class Product { ... }
```
- Schema: `@Table(name = "products", schema = "cinema")`.
- Cột:
  - `productId`: `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "product_id")`
  - `productName`: `@Column(name = "product_name", nullable = false)`
  - `description`: `@Column(name = "description")`
  - `price`: `@Column(name = "price", nullable = false)` (`BigDecimal`)
  - `category`: `@Column(name = "category")`
  - `imageUrl`: `@Column(name = "image_url")`
  - `status`: `@Enumerated(EnumType.STRING) @Column(name = "status")` (`ProductStatus`)

### 3.3 Response DTO `ProductResponse`
```java
package com.cinema.product;

public class ProductResponse { ... }
```
Ánh xạ đúng định dạng `Product` trong Swagger:
- `productId` (`Long`)
- `productName` (`String`)
- `description` (`String`)
- `price` (`BigDecimal`)
- `category` (`String`)
- `imageUrl` (`String`)
- `status` (`ProductStatus`)

Metadata phân trang tái sử dụng `com.cinema.common.dto.CommonDTO.PageMeta`.

### 3.4 DAO `ProductDAO`
Các phương thức chính:
- `findWithPaging(String category, ProductStatus status, int page, int size)`: Trả về danh sách `List<Product>` có phân trang và điều kiện lọc động (JPQL).
- `countTotal(String category, ProductStatus status)`: Đếm tổng số bản ghi phù hợp điều kiện lọc.
- `findById(Long productId)`: Hỗ trợ tìm kiếm theo ID.

### 3.5 Service `ProductService`
- Phương thức `getProducts(String category, String statusStr, int page, int size)`:
  - Parse `statusStr` an toàn sang `ProductStatus` (nếu không hợp lệ ném `ProductException` hoặc bỏ qua).
  - Chuẩn hóa `page >= 0`, `size > 0` (mặc định size tối đa 50/100 để tránh quét DB quá tải).
  - Gọi DAO lấy `items` và `totalElements`.
  - Tính `totalPages = (int) Math.ceil((double) totalElements / size)`.
  - Đóng gói dữ liệu cùng `PageMeta` và trả về kết quả.

### 3.6 Controller `ProductController`
- `@WebServlet(name = "ProductController", urlPatterns = {"/products", "/products/*"})`
- Xử lý method `doGet`:
  - Lấy query param: `category`, `status`, `page`, `size`.
  - Gọi `ProductService.getProducts(...)`.
  - Phản hồi JSON `200 OK` qua `ApiResponse.ok(pageData)`.

---

## 4. Các Bước Triển Khai

1. **Bước 1**: Tạo Enum `ProductStatus` và Entity `Product.java`.
2. **Bước 2**: Đăng ký Entity `com.cinema.product.Product` vào file [persistence.xml](file:///d:/IT1_UTE/HK5_26_27/lap_trinh_web/project/cinema_system/src/main/resources/META-INF/persistence.xml).
3. **Bước 3**: Tạo các DTO (`ProductResponse.java`) và Exception (`ProductException.java`).
4. **Bước 4**: Tạo lớp `ProductDAO.java` xử lý JPQL động với phân trang.
5. **Bước 5**: Tạo lớp `ProductService.java` xử lý nghiệp vụ.
6. **Bước 6**: Tạo `ProductController.java` phục vụ endpoint `GET /products`.
7. **Bước 7**: Viết Unit Test `ProductModuleTest.java` và tạo file Postman `product_postman.json` + `POSTMAN_GUIDE.md`.
8. **Bước 8**: Chạy `mvnw test` để đảm bảo toàn bộ mã nguồn build thành công và vượt qua tất cả test cases.
