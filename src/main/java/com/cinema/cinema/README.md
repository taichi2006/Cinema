# Tài Liệu Kỹ Thuật: Module Cinema (`com.cinema.cinema`)

Tài liệu kỹ thuật mô tả kiến trúc, endpoint API, cấu trúc dữ liệu, cơ chế phân trang, tìm kiếm và giải pháp tối ưu truy vấn CSDL của module rạp chiếu phim (Cinema).

---

## 1. Cấu Trúc Thành Phần

```text
com.cinema.cinema/
├── controller/
│   └── CinemaController.java             # Servlet tiếp nhận HTTP GET /cinemas, điều phối URL Dispatcher
├── dao/
│   └── CinemaDAO.java                    # Tầng truy vấn CSDL JPA/Hibernate (JPQL & Native SQL)
├── dto/
│   ├── request/
│   │   └── CinemaRequest.java            # DTO đóng gói tham số query (city, status, page, size)
│   └── response/
│       └── CinemaResponse.java           # DTO dữ liệu rạp trả về client (7 trường)
├── entity/
│   └── Cinema.java                       # Entity ánh xạ bảng cinema.cinemas (7 cột)
├── service/
│   └── CinemaService.java                # Xử lý validation, phân trang và mapping DTO
└── README.md                             # Tài liệu kỹ thuật module
```

---

## 2. Đặc Tả Cơ Sở Dữ Liệu (`cinema.cinemas`)

Bảng dữ liệu thực tế trong schema `cinema`:
```sql
CREATE TABLE cinema.cinemas (
    cinema_id    SERIAL PRIMARY KEY,
    cinema_name  VARCHAR(255) NOT NULL,
    address      TEXT,
    city         VARCHAR(100),
    phone        VARCHAR(20),
    email        VARCHAR(255),
    status       VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','INACTIVE','MAINTENANCE'))
);
```

---

## 3. Đặc Tả API

### 3.1. Danh Sách Rạp Chiếu (`GET /cinemas`)
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/cinemas", "/cinemas/*", "/cinema", "/cinema/*"})`
- **Quyền truy cập:** Public
- **Query Parameters:**
  - `city` (string, optional): Lọc theo tên thành phố (ví dụ: `Hồ Chí Minh`, `Hà Nội`).
  - `status` (string, optional): Enum `[ACTIVE, INACTIVE, MAINTENANCE]`.
  - `page` (int, default `0`): Chỉ số trang ($\ge 0$).
  - `size` (int, default `20`): Kích thước trang ($[1, 50]$).

- **Phản Hồi Thành Công (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "cinemaId": 1,
        "cinemaName": "Galaxy Nguyễn Du",
        "address": "116 Nguyễn Du, Quận 1, TP.HCM",
        "city": "Hồ Chí Minh",
        "phone": "028 3823 4567",
        "email": "contact@galaxy.vn",
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

---

### 3.2. Chi Tiết Rạp Chiếu (`GET /cinemas/{cinemaId}`)
- **Path Parameter:** `cinemaId` (Long, bắt buộc, số nguyên dương).
- **Phản Hồi Thành Công (HTTP 200 OK):**
```json
{
  "success": true,
  "data": {
    "cinemaId": 1,
    "cinemaName": "Galaxy Nguyễn Du",
    "address": "116 Nguyễn Du, Quận 1, TP.HCM",
    "city": "Hồ Chí Minh",
    "phone": "028 3823 4567",
    "email": "contact@galaxy.vn",
    "status": "ACTIVE"
  }
}
```
- **Phản Hồi Không Tìm Thấy (HTTP 404 Not Found):**
```json
{
  "success": false,
  "status": 404,
  "error": "Khong tim thay rap"
}
```

---

### 3.3. Lịch Chiếu Tại Rạp (`GET /cinemas/{cinemaId}/showtimes`)
- **Path Parameter:** `cinemaId` (Long, bắt buộc).
- **Query Parameters:**
  - `date` (string, **bắt buộc**, format `YYYY-MM-DD`).
  - `movieId` (int/long, tùy chọn): Lọc theo mã phim cụ thể.
- **Phản Hồi Thành Công (HTTP 200 OK):** Mảng trực tiếp trong `data` ([`ShowtimeResponse`](file:///c:/Users/khong/Desktop/TaiLieuHocTap/HK5_Nam3/WebProgramming/Cinema/src/main/java/com/cinema/movie/dto/response/ShowtimeResponse.java)):
```json
{
  "success": true,
  "data": [
    {
      "showTimeId": 1,
      "movieId": 1,
      "roomId": 1,
      "showDate": "2026-10-10",
      "startTime": "19:00:00",
      "endTime": "21:30:00",
      "basePrice": 90000.0,
      "status": "SCHEDULED"
    }
  ]
}
```
- **Phản Hồi Lỗi Khi Thiếu `date` (HTTP 400 Bad Request):**
```json
{
  "success": false,
  "status": 400,
  "error": "Tham so date la bat buoc"
}
```
- **Phản Hồi Lỗi Khi Rạp Không Tồn Tại (HTTP 404 Not Found):**
```json
{
  "success": false,
  "status": 404,
  "error": "Khong tim thay rap"
}
```

---

## 4. Tích Hợp Trang Chủ (`HomeService`)

Endpoint `/home` sử dụng `CinemaService.getCinemas(new CinemaRequest(null, "ACTIVE", 0, 10))` và `CinemaService.getCities()`:
- Trích xuất danh sách `items` từ `cinemaResult.get("data")` an toàn qua hàm helper `extractCinemaItems`.
- Danh sách thành phố `cities` được lấy qua truy vấn DISTINCT cột `city` có `status = 'ACTIVE'`.
