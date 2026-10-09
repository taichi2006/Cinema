# Tài Liệu Kỹ Thuật: Module Movie (`com.cinema.movie`)

Tài liệu kỹ thuật mô tả kiến trúc, endpoint API, cấu trúc dữ liệu, cơ chế phân trang và giải pháp tối ưu truy vấn CSDL của module phim.

---

## 1. Cấu Trúc Thành Phần

```text
com.cinema.movie/
├── controller/
│   └── MovieController.java        # Servlet tiếp nhận HTTP GET /movie
├── dao/
│   └── MovieDAO.java               # Truy vấn CSDL JPA/Hibernate & Native SQL
├── dto/
│   ├── request/
│   │   └── MovieRequest.java       # DTO đóng gói tham số query & pagination
│   └── response/
│       ├── MovieResponse.java      # DTO dữ liệu phim trả về client
│       ├── ShowtimeResponse.java   # DTO dữ liệu suất chiếu trả về client
│       └── ReviewResponse.java     # DTO dữ liệu đánh giá phim trả về client
├── entity/
│   ├── Movie.java                  # Entity ánh xạ bảng cinema.movies
│   └── Genre.java                  # Entity ánh xạ bảng cinema.genres (Many-to-Many)
├── service/
│   └── MovieService.java           # Xử lý validation, phân trang và mapping DTO
└── README.md                       # Tài liệu kỹ thuật module
```

---

## 2. Đặc Tả API: Lấy Danh Sách Phim

- **Endpoint:** `GET /api/movie`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/movie", "/movie/*"})` (chuyển tiếp qua `ApiPrefixFilter`)
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

Cục Success (`success`, `data`) ở trên và `meta` ([`CommonDTO.PageMeta`](file:///c:/Users/khong/Desktop/TaiLieuHocTap/HK5_Nam3/WebProgramming/Cinema/src/main/java/com/cinema/common/dto/CommonDTO.java#L39)) ở dưới:

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

- Lỗi tham số: Ném `ApiException.badRequest(...)` (HTTP status 400).
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

---

## 4. Đặc Tả API: Chi Tiết Một Phim

- **Endpoint:** `GET /api/movie/{id}`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/movie", "/movie/*"})` (chuyển tiếp qua `ApiPrefixFilter`)
- **Quyền truy cập:** Public
- **Content-Type:** `application/json;charset=UTF-8`

### 4.1. Path Parameters

| Tham số | Kiểu dữ liệu | Bắt buộc | Ràng buộc kỹ thuật |
| :--- | :--- | :--- | :--- |
| `id` | `String` / `Long` | Có | Bắt buộc là số nguyên dương $\ge 1$. Sai định dạng ném lỗi 400. |

### 4.2. Phản Hồi Thành Công (HTTP 200 OK)

Đóng gói qua `CommonDTO.ApiResponse<MovieResponse>`:

```json
{
  "success": true,
  "message": null,
  "data": {
    "id": "1",
    "title": "Mai",
    "description": "Phim tâm lý tình cảm gia đình",
    "durationMinutes": 120,
    "releaseDate": "2024-02-10",
    "posterUrl": "https://example.com/poster.jpg",
    "trailerUrl": "https://example.com/trailer.mp4",
    "language": "VI",
    "defaultFormat": "2D",
    "ageRating": "T18",
    "ageLimit": 18,
    "status": "NOW_SHOWING",
    "genres": ["Tâm lý", "Hài"],
    "averageRating": 0.0,
    "reviewCount": 0
  }
}
```

### 4.3. Phản Hồi Lỗi

Xử lý tập trung qua `com.cinema.common.exception.ErrorHandler`:

- **Không tìm thấy phim (HTTP 404 Not Found):** Khi `id` không tồn tại trong CSDL.
  ```json
  {
    "success": false,
    "status": 404,
    "error": "Không tìm thấy phim"
  }
  ```
- **Mã ID không hợp lệ (HTTP 400 Bad Request):** Khi `id` là chữ (vd: `"abc"`), số âm hoặc để trống.
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Mã phim 'id' không hợp lệ: abc"
  }
  ```

### 4.4. Kỹ Thuật Truy Vấn CSDL: `findById` (`MovieDAO.java`)

Đối với truy vấn 1 bản ghi cụ thể theo ID, không bị ảnh hưởng bởi giới hạn phân trang bộ nhớ, `MovieDAO` sử dụng câu JPQL nạp kèm `genres` chỉ trong **1 câu truy vấn đơn**, giải quyết triệt để vấn đề N+1 query:

```sql
SELECT DISTINCT m FROM Movie m LEFT JOIN FETCH m.genres WHERE m.movieId = :movieId
```

---

## 5. Kiến Trúc Định Tuyến Mở Rộng (URL Dispatcher)

`MovieController` sử dụng cơ chế Dispatcher phân tích đường dẫn `pathInfo` theo các phân đoạn (segments), hỗ trợ đồng thời các endpoint hiện tại và chuẩn bị sẵn khung cho các tính năng tiếp theo:

```text
GET /api/movie
 ├── /                          ──> handleGetMovies()          [Danh sách phim phân trang]
 └── /{id}
      ├── (không có hậu tố)     ──> handleGetMovieDetail(id)   [Chi tiết 1 bộ phim]
      ├── /showtime             ──> handleGetMovieShowtimes(id)[Danh sách suất chiếu theo ngày & rạp]
      └── /review               ──> handleGetMovieReviews(id)  [Đọc đánh giá công khai của phim]
```

---

## 6. Đặc Tả API: Danh Sách Suất Chiếu Của Phim

- **Endpoint:** `GET /api/movie/{id}/showtime`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/movie", "/movie/*"})` (chuyển tiếp qua `ApiPrefixFilter`)
- **Quyền truy cập:** Public
- **Content-Type:** `application/json;charset=UTF-8`

### 6.1. Tham Số Yêu Cầu

| Tham số | Vị trí | Kiểu | Bắt buộc | Mặc định | Ràng buộc kỹ thuật |
| :--- | :--- | :--- | :---: | :---: | :--- |
| `id` | Path | `String` / `Long` | **Có** | - | Mã phim. Phải là số nguyên dương $\ge 1$. |
| `date` | Query | `String` (`date`) | **Có** | - | Ngày chiếu định dạng `YYYY-MM-DD`. Thiếu hoặc sai định dạng ném lỗi 400. |
| `cinemaId` | Query | `String` / `Long` | Không | `null` | Lọc theo cụm rạp. Nếu có, phải là số nguyên dương $\ge 1$. |
| `page` | Query | `int` | Không | `0` | Chỉ số trang, $\ge 0$. |
| `size` | Query | `int` | Không | `20` | Số lượng bản ghi trên một trang, trong khoảng $[1, 50]$. |

### 6.2. Phản Hồi Thành Công (HTTP 200 OK)

Cục Success (`success`, `data`) ở trên và `meta` ([`CommonDTO.PageMeta`](file:///c:/Users/khong/Desktop/TaiLieuHocTap/HK5_Nam3/WebProgramming/Cinema/src/main/java/com/cinema/common/dto/CommonDTO.java#L39)) ở dưới:

```json
{
  "success": true,
  "data": [
    {
      "id": "101",
      "movieId": "1",
      "cinemaId": "1",
      "cinemaName": "Galaxy Nguyễn Du",
      "roomId": "5",
      "roomName": "Cinema 1",
      "startsAt": "2026-10-05T14:30:00Z",
      "endsAt": "2026-10-05T16:30:00Z",
      "format": "2D",
      "language": "VI",
      "basePrice": 95000,
      "status": "OPEN"
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

- `data`: Mảng danh sách các suất chiếu (`ShowtimeResponse`). Chỉ lấy các suất chiếu đang mở bán (`status = 'OPEN'`) của ngày được chọn.
- `startsAt`, `endsAt`: Định dạng chuỗi thời gian chuẩn ISO-8601.
- `meta`: Thông tin phân trang dùng chung `CommonDTO.PageMeta`.

### 6.3. Phản Hồi Lỗi

Xử lý tập trung qua `com.cinema.common.exception.ErrorHandler`:

- **Thiếu tham số `date` (HTTP 400 Bad Request):**
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Thiếu tham số bắt buộc: date"
  }
  ```
- **Sai định dạng `date` (HTTP 400 Bad Request):**
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Định dạng ngày 'date' không hợp lệ (yêu cầu: YYYY-MM-DD): 2026/10/05"
  }
  ```
- **Phim không tồn tại (HTTP 404 Not Found):**
  ```json
  {
    "success": false,
    "status": 404,
    "error": "Không tìm thấy phim"
  }
  ```
- **Mã rạp hoặc phân trang không hợp lệ (HTTP 400 Bad Request):**
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Mã rạp 'cinemaId' phải là số nguyên dương."
  }
  ```

### 6.4. Kỹ Thuật Truy Vấn CSDL: `findShowtimes` (`MovieDAO.java`)

`MovieDAO` sử dụng câu truy vấn Native SQL kết hợp 3 bảng `showtimes`, `rooms` và `cinemas`:

```sql
SELECT 
    s.show_time_id, s.movie_id, c.cinema_id, c.cinema_name,
    r.room_id, r.room_name, 
    (s.show_date + s.start_time) AS starts_at, 
    (s.show_date + s.end_time) AS ends_at,
    m.format, m.language, s.base_price, s.status
FROM cinema.show_times s
JOIN cinema.rooms r ON s.room_id = r.room_id
JOIN cinema.cinemas c ON r.cinema_id = c.cinema_id
JOIN cinema.movies m ON s.movie_id = m.movie_id
WHERE s.movie_id = :movieId
  AND s.status = 'SCHEDULED'
  AND s.show_date = CAST(:showDate AS date)
  [AND c.cinema_id = :cinemaId]
ORDER BY s.show_date ASC, s.start_time ASC
LIMIT :size OFFSET :offset
```

---

## 7. Đặc Tả API: Đánh Giá Công Khai Của Phim

- **Endpoint:** `GET /api/movie/{id}/review`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/movie", "/movie/*"})` (chuyển tiếp qua `ApiPrefixFilter`)
- **Quyền truy cập:** Public
- **Content-Type:** `application/json;charset=UTF-8`

### 7.1. Tham Số Yêu Cầu

| Tham số | Vị trí | Kiểu | Bắt buộc | Mặc định | Ràng buộc kỹ thuật |
| :--- | :--- | :--- | :---: | :---: | :--- |
| `id` | Path | `String` / `Long` | **Có** | - | Mã phim. Phải là số nguyên dương $\ge 1$. Nếu không tìm thấy phim ném 404. |
| `sort` | Query | `String` | Không | `createdAt,desc` | Thứ tự sắp xếp. Các giá trị hợp lệ: `createdAt,desc`, `createdAt,asc`, `rating,desc`, `rating,asc`. Giá trị khác ném lỗi 400. |
| `page` | Query | `int` | Không | `0` | Chỉ số trang, $\ge 0$. Sai kiểu hoặc âm ném lỗi 400. |
| `size` | Query | `int` | Không | `20` | Kích thước trang, $[1, 50]$. Ngoài khoảng ném lỗi 400. |

### 7.2. Phản Hồi Thành Công (HTTP 200 OK)

Cấu trúc flat JSON chuẩn hóa với `meta` ([`CommonDTO.PageMeta`](file:///c:/Users/khong/Desktop/TaiLieuHocTap/HK5_Nam3/WebProgramming/Cinema/src/main/java/com/cinema/common/dto/CommonDTO.java#L39)):

```json
{
  "success": true,
  "data": [
    {
      "id": "10",
      "movieId": "1",
      "authorDisplayName": "Nguyễn Văn A",
      "rating": 5,
      "comment": "Phim rất hay, cảm động!",
      "createdAt": "2026-10-04T08:08:17.626Z"
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

- `data`: Mảng danh sách đánh giá (`ReviewResponse`).
  - `id`: Mã định danh review (`ur.review_id`).
  - `movieId`: Mã phim (`s.movie_id`).
  - `authorDisplayName`: Tên hiển thị của tác giả đánh giá, trích xuất từ `u.full_name` qua liên kết với bảng `cinema.users`.
  - `rating`: Số sao đánh giá từ 1 đến 5 (`ur.rating`).
  - `comment`: Nội dung nhận xét của khán giả (`ur.comment`).
  - `createdAt`: Chuỗi thời gian định dạng ISO-8601.
- `meta`: Thông tin phân trang dùng chung `CommonDTO.PageMeta` (`page`, `size`, `totalElements`, `totalPages`).

### 7.3. Phản Hồi Lỗi

Xử lý tập trung qua `com.cinema.common.exception.ErrorHandler`:

- **Phim không tồn tại (HTTP 404 Not Found):**
  ```json
  {
    "success": false,
    "status": 404,
    "error": "Không tìm thấy phim"
  }
  ```
- **Tham số `sort` không hợp lệ (HTTP 400 Bad Request):**
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Tham số 'sort' không hợp lệ: invalid. Các giá trị hợp lệ: [createdAt,asc, createdAt,desc, rating,asc, rating,desc]"
  }
  ```
- **Phân trang hoặc `id` không hợp lệ (HTTP 400 Bad Request):**
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50."
  }
  ```

### 7.4. Kỹ Thuật Truy Vấn CSDL: `findReviews` & `countReviews` (`MovieDAO.java`)

Trong CSDL PostgreSQL, bảng `cinema.reviews` kết nối trực tiếp với bảng `cinema.users` (chứa tên tác giả `full_name`).

`MovieDAO` sử dụng Native SQL kết hợp 2 bảng để truy vấn trực tiếp mà không cần khởi tạo Entity JPA mới ngoài module `movie`:

```sql
SELECT 
    ur.review_id,
    s.movie_id,
    u.full_name,
    ur.rating,
    ur.comment,
    ur.created_at
FROM cinema.reviews ur
JOIN cinema.users u ON ur.user_id = u.user_id
WHERE ur.movie_id = :movieId
ORDER BY ...
LIMIT :size OFFSET :offset
```

- **Sắp xếp linh hoạt:**
  - `createdAt,desc` $\rightarrow$ `ORDER BY ur.created_at DESC`
  - `createdAt,asc` $\rightarrow$ `ORDER BY ur.created_at ASC`
  - `rating,desc` $\rightarrow$ `ORDER BY ur.rating DESC, ur.created_at DESC`
  - `rating,asc` $\rightarrow$ `ORDER BY ur.rating ASC, ur.created_at DESC`

- **Truy vấn đếm tổng (`countReviews`):**
```sql
SELECT COUNT(*)
FROM cinema.reviews ur
WHERE ur.movie_id = :movieId
```

---

## 8. Kiểm Thử Đơn Vị (Unit Testing)

Tất cả các kịch bản nghiệp vụ của module `movie` được kiểm thử tự động trong `MovieServiceTest.java` thông qua kỹ thuật mock DAO (`StubMovieDAO`):

- **Tổng số tests:** 24 test cases.
- **Tỷ lệ vượt qua:** 100% PASS (`BUILD SUCCESS`).
- **Phạm vi kiểm thử:**
  1. `getMovies`: Tìm kiếm `q`, lọc theo `genre`, `status`, sắp xếp `sort`, phân trang `page`/`size`, xử lý tràn số `offset`, gán mặc định `releaseDate,desc`, map danh sách thể loại không trùng lặp.
  2. `getMovieById`: Thành công, chi tiết phim đủ trường, phim không tồn tại (404), ID âm / chứa ký tự / để trống (400).
  3. `getMovieShowtimes`: Lọc theo `date`, lọc theo `cinemaId`, phân trang `page`/`size`, validate định dạng `date`, validate `cinemaId`, phim không tồn tại (404).
  4. `getMovieReviews`: Lấy danh sách đánh giá thành công, kiểm tra các trường `ReviewResponse`, kiểm tra `PageMeta`, validate các kiểu `sort`, validate `page`/`size`, phim không tồn tại (404), danh sách đánh giá rỗng (`totalElements = 0`).
