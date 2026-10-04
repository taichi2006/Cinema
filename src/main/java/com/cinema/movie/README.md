# Tài Liệu Kỹ Thuật: Module Movie (`com.cinema.movie`)

Tài liệu kỹ thuật mô tả kiến trúc, endpoint API, cấu trúc dữ liệu, cơ chế phân trang và giải pháp tối ưu truy vấn CSDL của module phim.

---

## 1. Cấu Trúc Thành Phần

```text
com.cinema.movie/
├── MovieController.java        # Servlet tiếp nhận HTTP GET /movie và /movies
├── MovieService.java           # Xử lý validation, phân trang và mapping DTO
├── MovieDAO.java               # Truy vấn CSDL JPA/Hibernate (Two-Step Fetch)
├── Movie.java                  # Entity ánh xạ bảng cinema.movies
├── Genre.java                  # Entity ánh xạ bảng cinema.genres (Many-to-Many)
├── MovieRequest.java           # DTO đóng gói tham số query & pagination
├── MovieResponse.java          # DTO dữ liệu từng bộ phim trả về client
├── SuccessEnvelope.java        # Wrapper response thành công (success, data, meta)
├── InvalidFilterException.java # Exception nghiệp vụ (kế thừa ApiException, HTTP 400)
└── README.md                   # Tài liệu kỹ thuật module
```

---

## 2. Đặc Tả API: Lấy Danh Sách Phim

- **Endpoint:** `GET /api/movie` hoặc `GET /api/movies`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/movie", "/movies"})` (chuyển tiếp qua `ApiPrefixFilter`)
- **Quyền truy cập:** Public (không yêu cầu JWT token qua `AuthFilter`)
- **Content-Type:** `application/json;charset=UTF-8`

### 2.1. Query Parameters (`MovieRequest`)

| Tham số | Kiểu dữ liệu | Mặc định | Ràng buộc kỹ thuật |
| :--- | :--- | :--- | :--- |
| `q` | `String` | `null` | Tối đa 100 ký tự. Tìm kiếm `LOWER(title) LIKE %q%`. |
| `genre` | `String` | `null` | Khớp không phân biệt hoa thường theo `genreCode` hoặc `genreName`. |
| `status` | `String` | `null` | Thuộc tập enum: `NOW_SHOWING`, `COMING_SOON`, `ENDED`. |
| `sort` | `String` | `releaseDate,desc` | Thuộc tập enum: `releaseDate,asc`, `releaseDate,desc`, `title,asc`, `title,desc`. |
| `page` | `int` | `0` | Giá trị $\ge 0$. |
| `size` | `int` | `20` | Giá trị trong khoảng $[1, 50]$. |

*(Lưu ý: Hệ thống kiểm tra tràn số: `page * size` không được vượt quá `Integer.MAX_VALUE`).*

### 2.2. Phản Hồi Thành Công (HTTP 200 OK)

Đóng gói qua `SuccessEnvelope<List<MovieResponse>>` và `CommonDTO.PageMeta`:

```json
{
  "success": true,
  "data": [
    {
      "id": "1",
      "title": "Mai",
      "posterUrl": "https://example.com/poster.jpg",
      "durationMinutes": 120,
      "releaseDate": "2026-10-03",
      "genres": ["Tâm lý", "Hài"],
      "status": "NOW_SHOWING",
      "ageRating": "T18",
      "averageRating": 0.0,
      "reviewCount": 0
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

- `id`: Định dạng kiểu chuỗi `String` theo chuẩn REST.
- `genres`: Danh sách tên tiếng Việt của thể loại, loại bỏ trùng lặp bằng `LinkedHashSet`.
- `meta`: Sử dụng trực tiếp lớp dùng chung `com.cinema.common.dto.CommonDTO.PageMeta`.

### 2.3. Phản Hồi Lỗi (HTTP 400 / 500)

Được xử lý tập trung qua `com.cinema.common.exception.ErrorHandler`:

```json
{
  "success": false,
  "status": 400,
  "error": "Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50."
}
```

- Lỗi tham số: Ném `InvalidFilterException` (kế thừa `ApiException` với status 400).
- Lỗi không xác định: Trả về HTTP status 500.

---

## 3. Kỹ Thuật Truy Vấn CSDL: Two-Step Fetch (`MovieDAO.java`)

Khi thực hiện phân trang trên thực thể có quan hệ `@ManyToMany` (`Movie` $\leftrightarrow$ `Genre`), việc dùng `setFirstResult`/`setMaxResults` kết hợp trực tiếp `JOIN FETCH m.genres` sẽ khiến Hibernate tải toàn bộ dữ liệu vào RAM để phân trang thủ công và phát cảnh báo bộ nhớ (`HHH000104`).

Để khắc phục triệt để, `MovieDAO` áp dụng quy trình 2 bước:

1. **Bước 1 (Phân trang ID):**
   - Thực hiện câu JPQL lọc theo `q`, `genre`, `status` và `sort`:
     ```sql
     SELECT DISTINCT m FROM Movie m [JOIN m.genres g] WHERE ... ORDER BY ...
     ```
   - Áp dụng `setFirstResult(page * size)` và `setMaxResults(size)`.
   - Trích xuất danh sách `List<Long> movieIds`.
2. **Bước 2 (Eager Fetch Collection):**
   - Thực hiện truy vấn thứ hai nạp kèm quan hệ `genres` chỉ cho các ID ở bước 1:
     ```sql
     SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres WHERE m.movieId IN (:movieIds)
     ```
3. **Bước 3 (Re-ordering):**
   - Đưa kết quả bước 2 vào `Map<Long, Movie>` và sắp xếp lại theo đúng thứ tự của danh sách ID ở bước 1 trước khi trả về `MovieService`.
4. **Truy vấn đếm tổng (`countMovies`):**
   - Chạy riêng `SELECT COUNT(DISTINCT m) FROM Movie m [JOIN m.genres g] WHERE ...` để lấy tổng số bản ghi phục vụ tính toán `totalPages`.
