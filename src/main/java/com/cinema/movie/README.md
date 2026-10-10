# Tài Liệu Kỹ Thuật: Module Movie (`com.cinema.movie`)

Tài liệu kỹ thuật mô tả kiến trúc, endpoint API, cấu trúc dữ liệu, cơ chế phân trang và giải pháp truy vấn CSDL của module phim theo chuẩn OpenAPI mới nhất.

---

## 1. Cấu Trúc Thành Phần

```text
com.cinema.movie/
├── controller/
│   └── MovieController.java        # Servlet tiếp nhận HTTP GET/POST /movies
├── dao/
│   └── MovieDAO.java               # Truy vấn CSDL JPA/Hibernate & Native SQL
├── dto/
│   ├── request/
│   │   ├── MovieRequest.java       # DTO đóng gói tham số query (keyword, genreId, status, page, size, sort)
│   │   └── ReviewRequest.java      # DTO dữ liệu gửi đánh giá phim (rating, comment)
│   └── response/
│       ├── MovieResponse.java      # DTO dữ liệu phim (11 trường chuẩn schema Movie)
│       ├── ShowtimeResponse.java   # DTO dữ liệu suất chiếu (8 trường chuẩn schema Showtime)
│       └── ReviewResponse.java     # DTO dữ liệu đánh giá phim (6 trường chuẩn schema Review)
├── entity/
│   ├── Movie.java                  # Entity ánh xạ bảng cinema.movies
│   └── Genre.java                  # Entity ánh xạ bảng cinema.genres (Many-to-Many)
├── service/
│   └── MovieService.java           # Xử lý validation, phân trang, điều kiện vé và mapping DTO
└── README.md                       # Tài liệu kỹ thuật module
```

---

## 2. Đặc Tả API: Danh Sách Phim (`GET /movies`)

- **Endpoint:** `GET /movies`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/movies", "/movies/*"})`
- **Quyền truy cập:** Public (không yêu cầu JWT token)
- **Content-Type:** `application/json;charset=UTF-8`

### 2.1. Query Parameters (`MovieRequest`)

| Tham số | Kiểu dữ liệu | Bắt buộc | Mặc định | Mô tả & Ràng buộc kỹ thuật |
| :--- | :--- | :---: | :--- | :--- |
| `keyword` | `String` | Không | `null` | Từ khóa tìm kiếm theo tên phim (`title`, `originalTitle`). Tối đa 100 ký tự. Hỗ trợ fallback sang `q`. |
| `genreId` | `Integer` | Không | `null` | Mã định danh thể loại phim (phải là số nguyên dương $\ge 1$). |
| `status` | `String` | Không | `null` | Trạng thái phim, phải thuộc enum: `[ACTIVE, INACTIVE, COMING_SOON, ENDED]`. Giá trị khác ném lỗi 400. |
| `page` | `int` | Không | `0` | Chỉ số trang, $\ge 0$. |
| `size` | `int` | Không | `20` | Số lượng phần tử mỗi trang, trong khoảng $[1, 50]$. |
| `sort` | `String` | Không | `releaseDate,desc` | Thứ tự sắp xếp (`releaseDate,asc`, `releaseDate,desc`, `title,asc`, `title,desc`). |

### 2.2. Phản Hồi Thành Công (HTTP 200 OK)

Trả về `SuccessEnvelope` kết hợp `PageOfMovie` gồm `items` và `meta` nằm trong `data`:

```json
{
  "success": true,
  "data": {
    "items": [
      {
        "movieId": 1,
        "title": "Dune: Hành Tinh Cát - Phần Hai",
        "directorId": 1,
        "durationMinutes": 166,
        "ageLimit": 16,
        "format": "2D",
        "description": "Hành trình tiếp theo của Paul Atreides...",
        "language": "VI",
        "posterUrl": "https://example.com/poster.jpg",
        "releaseDate": "2024-03-01",
        "status": "ACTIVE"
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

### 2.3. Phản Hồi Lỗi (HTTP 400 Bad Request)

Khi tham số `status` không nằm trong danh sách enum hợp lệ:

```json
{
  "success": false,
  "status": 400,
  "error": "Trang thai phim khong hop le"
}
```

---

## 3. Đặc Tả API: Chi Tiết Phim (`GET /movies/{movieId}`)

- **Endpoint:** `GET /movies/{movieId}`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/movies", "/movies/*"})`
- **Quyền truy cập:** Public
- **Content-Type:** `application/json;charset=UTF-8`

### 3.1. Path Parameters

| Tham số | Kiểu dữ liệu | Bắt buộc | Ràng buộc kỹ thuật |
| :--- | :--- | :---: | :--- |
| `movieId` | `Long` | **Có** | Mã phim. Phải là số nguyên dương $\ge 1$. Sai định dạng ném lỗi 400. |

### 3.2. Phản Hồi Thành Công (HTTP 200 OK)

Trả về `SuccessEnvelope` với `data` là đối tượng `Movie` (11 trường):

```json
{
  "success": true,
  "data": {
    "movieId": 1,
    "title": "Dune: Hành Tinh Cát - Phần Hai",
    "directorId": 1,
    "durationMinutes": 166,
    "ageLimit": 16,
    "format": "2D",
    "description": "Hành trình tiếp theo của Paul Atreides...",
    "language": "VI",
    "posterUrl": "https://example.com/poster.jpg",
    "releaseDate": "2024-03-01",
    "status": "ACTIVE"
  }
}
```

### 3.3. Phản Hồi Lỗi (HTTP 404 Not Found)

Khi không tìm thấy phim tương ứng với `movieId`:

```json
{
  "success": false,
  "status": 404,
  "error": "Khong tim thay phim"
}
```

---

## 4. Đặc Tả API: Lịch Chiếu Của Phim (`GET /movies/{movieId}/showtimes`)

- **Endpoint:** `GET /movies/{movieId}/showtimes`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/movies", "/movies/*"})`
- **Quyền truy cập:** Public
- **Content-Type:** `application/json;charset=UTF-8`

### 4.1. Tham Số Yêu Cầu

| Tham số | Vị trí | Kiểu | Bắt buộc | Mặc định | Ràng buộc kỹ thuật |
| :--- | :--- | :--- | :---: | :---: | :--- |
| `movieId` | Path | `Long` | **Có** | - | Mã phim $\ge 1$. Không tìm thấy ném lỗi 404 `"Khong tim thay phim"`. |
| `date` | Query | `String` (`date`) | **Có** | - | Ngày chiếu `YYYY-MM-DD`. Thiếu ném 400 `"Tham so date la bat buoc"`. |
| `cinemaId` | Query | `Integer` | Không | `null` | Lọc theo cụm rạp. Nếu có, phải là số nguyên dương $\ge 1$. |

### 4.2. Phản Hồi Thành Công (HTTP 200 OK)

Trả về `SuccessEnvelope` chứa mảng các suất chiếu `Showtime` (chuẩn schema 8 trường):

```json
{
  "success": true,
  "data": [
    {
      "showTimeId": 101,
      "movieId": 1,
      "roomId": 5,
      "showDate": "2026-10-09",
      "startTime": "14:30:00",
      "endTime": "16:30:00",
      "basePrice": 95000,
      "status": "OPEN"
    }
  ]
}
```

### 4.3. Phản Hồi Lỗi

- **Thiếu tham số `date` (HTTP 400 Bad Request):**
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Tham so date la bat buoc"
  }
  ```

- **Không tìm thấy phim (HTTP 404 Not Found):**
  ```json
  {
    "success": false,
    "status": 404,
    "error": "Khong tim thay phim"
  }
  ```

---

## 5. Đặc Tả API: Đánh Giá Phim (`/movies/{movieId}/reviews`)

### 5.1. Lấy Danh Sách Đánh Giá (`GET /movies/{movieId}/reviews`)

- **Endpoint:** `GET /movies/{movieId}/reviews`
- **Quyền truy cập:** Public
- **Tham số:**
  - `movieId` (Path, bắt buộc): ID phim.
  - `page` (Query, mặc định 0): Chỉ số trang.
  - `size` (Query, mặc định 20): Số bản ghi mỗi trang.
- **Phản Hồi Thành Công (HTTP 200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "items": [
        {
          "reviewId": 1,
          "movieId": 1,
          "userId": 2,
          "rating": 5,
          "comment": "Phim hay, plot twist dinh cao!",
          "createdAt": "2026-10-10T02:04:04.519Z"
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
- **Phản Hồi Lỗi:**
  - `404 Not Found`: Khi không tìm thấy phim (`"Khong tim thay phim"`).

### 5.2. Tạo / Cập Nhật Đánh Giá (`POST /movies/{movieId}/reviews`)

- **Endpoint:** `POST /movies/{movieId}/reviews`
- **Quyền truy cập:** Bắt buộc đăng nhập (`Authorization: Bearer <token>`)
- **Request Body (`ReviewRequest`):**
  ```json
  {
    "rating": 5,
    "comment": "Phim hay, plot twist dinh cao!"
  }
  ```
- **Phản Hồi Thành Công (HTTP 200 OK):**
  ```json
  {
    "success": true,
    "data": {
      "reviewId": 1,
      "movieId": 1,
      "userId": 1,
      "rating": 5,
      "comment": "Phim hay, plot twist dinh cao!",
      "createdAt": "2024-07-15T18:30:00Z"
    }
  }
  ```
- **Phản Hồi Lỗi:**
  - `400 Bad Request`:
    - Số sao không hợp lệ: `"So sao phai tu 1-5"`.
    - Bình luận vượt quá độ dài: `"Binh luan toi da 2000 ky tu"`.
  - `401 Unauthorized`: Chưa đăng nhập hoặc token sai định dạng (`"Chua dang nhap"`).
  - `404 Not Found`: Không tìm thấy phim (`"Khong tim thay phim"`).
  - `422 Unprocessable Entity`:
    - Người dùng chưa từng mua vé: `"Ban chua xem phim nay"`.
    - Đơn hàng chưa được thanh toán: `"Chi danh gia don da thanh toan"`.
    - Suất chiếu chưa kết thúc / chưa có vé `USED`: `"Chi danh gia sau khi suat chieu ket thuc"`.

---

## 6. Kỹ Thuật Truy Vấn CSDL (`MovieDAO.java`)

### 6.1. Phân Trang và Lọc Danh Sách (`findMovies` & `countMovies`)
- **Lọc theo `keyword`:** So khớp cả tên tiếng Việt và tên gốc:
  ```sql
  AND (LOWER(m.title) LIKE :keyword OR LOWER(m.originalTitle) LIKE :keyword)
  ```
- **Lọc theo `genreId`:** Sử dụng quan hệ `JOIN m.genres g WHERE g.genreId = :genreId`.
- **Lọc theo `status`:** Lọc theo trạng thái `ACTIVE`, `INACTIVE`, `COMING_SOON`, `ENDED`.
- **Quy trình Two-Step Fetch:** Tránh lỗi Hibernate `HHH000104` khi phân trang kèm quan hệ đa-đa `@ManyToMany`.

### 6.2. Lịch Chiếu Phim (`findShowtimes`)
- Kết nối `cinema.show_times s` và `cinema.rooms r`:
  ```sql
  SELECT s.show_time_id, s.movie_id, s.room_id, s.show_date, s.start_time, s.end_time, s.base_price, s.status
  FROM cinema.show_times s
  JOIN cinema.rooms r ON s.room_id = r.room_id
  WHERE s.movie_id = :movieId
    AND s.show_date = CAST(:showDate AS date)
    [AND r.cinema_id = :cinemaId]
  ORDER BY s.show_date ASC, s.start_time ASC
  ```

### 6.3. Đánh Giá Phim (`findReviews` & `upsertReview`)
- Truy vấn danh sách đánh giá:
  ```sql
  SELECT ur.review_id, ur.movie_id, ur.user_id, ur.rating, ur.comment, ur.created_at
  FROM cinema.reviews ur
  WHERE ur.movie_id = :movieId
  ORDER BY ur.created_at DESC
  ```
- Lưu / Cập nhật đánh giá nguyên tử (Atomic Upsert):
  ```sql
  INSERT INTO cinema.reviews (movie_id, user_id, rating, comment, created_at)
  VALUES (:movieId, :userId, :rating, :comment, CURRENT_TIMESTAMP)
  ON CONFLICT (user_id, movie_id)
  DO UPDATE SET rating = EXCLUDED.rating, comment = EXCLUDED.comment, created_at = CURRENT_TIMESTAMP
  RETURNING review_id, movie_id, user_id, rating, comment, created_at;
  ```

---

## 7. Kiểm Thử Đơn Vị (Unit Testing)

Tất cả các kịch bản nghiệp vụ của module `movie` được kiểm thử tự động trong `MovieServiceTest.java`:
- Phim: `testGetMovies_Success`, `testGetMovies_FilterKeywordAndGenre`, `testGetMovies_InvalidStatus`, `testGetMovieById_Success`, `testGetMovieById_NotFound`, `testGetMovieById_InvalidId`.
- Lịch chiếu: `testGetMovieShowtimes_Success`, `testGetMovieShowtimes_MissingDate`, `testGetMovieShowtimes_InvalidDateFormat`, `testGetMovieShowtimes_MovieNotFound`.
- Đánh giá (Reviews):
  - `testGetMovieReviews_Success`: Lấy danh sách review với cấu trúc `items` và `meta`.
  - `testGetMovieReviews_MovieNotFound`: Phim không tồn tại báo 404.
  - `testCreateOrUpdateReview_Success`: Lưu đánh giá thành công khi có vé `USED` (200 OK).
  - `testCreateOrUpdateReview_Unauthenticated`: Báo lỗi 401 `"Chua dang nhap"`.
  - `testCreateOrUpdateReview_MovieNotFound`: Báo lỗi 404 `"Khong tim thay phim"`.
  - `testCreateOrUpdateReview_InvalidRating`: Rating không trong khoảng 1-5 báo 400 `"So sao phai tu 1-5"`.
  - `testCreateOrUpdateReview_CommentTooLong`: Comment > 2000 ký tự báo 400 `"Binh luan toi da 2000 ky tu"`.
  - `testCreateOrUpdateReview_NotWatched`: Chưa mua vé báo 422 `"Ban chua xem phim nay"`.
  - `testCreateOrUpdateReview_NotPaid`: Đơn chưa thanh toán báo 422 `"Chi danh gia don da thanh toan"`.
  - `testCreateOrUpdateReview_NotEnded`: Chưa có vé `USED` báo 422 `"Chi danh gia sau khi suat chieu ket thuc"`.
