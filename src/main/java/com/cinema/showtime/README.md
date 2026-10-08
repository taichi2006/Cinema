# Tài Liệu Kỹ Thuật: Module Showtime (`com.cinema.showtime`)

Tài liệu kỹ thuật mô tả kiến trúc, endpoint API, cấu trúc sơ đồ ghế, trạng thái đặt chỗ theo thời gian thực và giải pháp tối ưu truy vấn CSDL của module suất chiếu & sơ đồ ghế.

---

## 1. Cấu Trúc Thành Phần

```text
com.cinema.showtime/
├── controller/
│   └── ShowtimeController.java                  # Servlet tiếp nhận HTTP GET /showtimes/{id} & /showtimes/{id}/seats
├── dao/
│   └── ShowtimeDAO.java                         # Tầng truy vấn CSDL Native SQL tối ưu
├── dto/
│   └── response/
│       ├── ShowtimeResponse.java                # DTO 8 trường thông tin suất chiếu (GET /showtimes/{id})
│       ├── SeatResponse.java                    # DTO 8 trường thông tin chi tiết từng ghế
│       └── SeatMapResponse.java                 # DTO envelope chứa showTimeId, seats
├── service/
│   └── ShowtimeService.java                     # Xử lý validation (400, 404) và nghiệp vụ suất chiếu
└── README.md                                    # Tài liệu kỹ thuật module
```

---

## 2. Đặc Tả API: Chi Tiết Suất Chiếu

- **Endpoint:** `GET /showtimes/{showTimeId}` (hoặc `GET /api/showtimes/{showTimeId}`)
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/showtimes", "/showtimes/*"})`
- **Quyền truy cập:** Public (`security: []`), không yêu cầu token
- **Content-Type:** `application/json;charset=UTF-8`

### 2.1. Phản Hồi Thành Công (HTTP 200 OK)
```json
{
  "success": true,
  "data": {
    "showTimeId": 1,
    "movieId": 1,
    "roomId": 1,
    "showDate": "2026-10-08",
    "startTime": "19:00:00",
    "endTime": "21:30:00",
    "basePrice": 90000.0,
    "status": "SCHEDULED"
  }
}
```

### 2.2. Phản Hồi Lỗi
- **HTTP 400 Bad Request:** Mã suất chiếu không phải là số nguyên dương.
- **HTTP 404 Not Found:** Không tìm thấy suất chiếu trong hệ thống:
```json
{
  "success": false,
  "status": 404,
  "error": "Khong tim thay suat chieu"
}
```

---

## 3. Đặc Tả API: Sơ Đồ Ghế Của Suất Chiếu

- **Endpoint:** `GET /showtimes/{showTimeId}/seats` (hỗ trợ thêm `/showtimes/{showTimeId}/seat`)
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/showtimes", "/showtimes/*"})`
- **Quyền truy cập:** Bắt buộc đăng nhập (`BearerAuth`), truyền qua header `Authorization: Bearer <accessToken>`
- **Content-Type:** `application/json;charset=UTF-8`
- **Mục đích:** Khách hàng xem sơ đồ ghế và tình trạng ghế theo thời gian thực (read-only).

### 3.1. Tham Số Đầu Vào

| Tham số | Vị trí | Kiểu | Bắt buộc | Ràng buộc kỹ thuật |
| :--- | :--- | :--- | :---: | :--- |
| `showTimeId` | Path | `Long` | **Có** | Mã suất chiếu. Phải là số nguyên dương $\ge 1$. Sai kiểu hoặc $\le 0$ $\rightarrow$ lỗi 400. |
| `Authorization` | Header | `String` | **Có** | Header chứa `Bearer <jwt_token>`. Thiếu header hoặc token không hợp lệ / hết hạn $\rightarrow$ lỗi 401. |

### 3.2. Phản Hồi Thành Công (HTTP 200 OK)

```json
{
  "success": true,
  "data": {
    "showTimeId": 1,
    "seats": [
      {
        "id": 1,
        "seatId": 1,
        "seatRow": "A",
        "seatCol": 1,
        "seatLabel": "A1",
        "price": 90000.0,
        "status": "AVAILABLE",
        "holdExpirationAt": null
      },
      {
        "id": 2,
        "seatId": 2,
        "seatRow": "A",
        "seatCol": 2,
        "seatLabel": "A2",
        "price": 90000.0,
        "status": "SELECTED",
        "holdExpirationAt": "2026-10-08T02:38:51.499Z"
      }
    ]
  }
}
```

### 3.3. Chi Tiết Dữ Liệu Phản Hồi

1. **`showTimeId`:** Mã suất chiếu (`Long`).
2. **`seats` (Mảng thông tin chi tiết từng ghế):**
   - `id`: Mã khóa chính bản ghi ghế theo suất chiếu (`cinema.show_time_seats.id`).
   - `seatId`: Mã ghế vật lý (`cinema.seats.seat_id`).
   - `seatRow`: Hàng ghế (ví dụ: `"A"`, `"B"`).
   - `seatCol`: Cột ghế (ví dụ: `1`, `2`).
   - `seatLabel`: Tên nhãn ghế hiển thị (ví dụ: `"A1"`, `"A2"`).
   - `price`: Giá vé của ghế (`Double`).
   - `status`: Trạng thái ghế tại thời điểm gọi API:
     - `"AVAILABLE"`: Ghế trống, hoặc ghế giữ chỗ đã hết hạn (`sts.status = 'SELECTED' AND sts.hold_expiration_at < CURRENT_TIMESTAMP`).
     - `"SELECTED"`: Ghế đang được giữ chỗ và còn hiệu lực.
     - `"BOOKED"`: Ghế đã được đặt/thanh toán.
   - `holdExpirationAt`: Thời điểm hết hạn giữ chỗ (ISO-8601). Trả về `null` nếu ghế đang trống hoặc đã hết hạn giữ chỗ.

---

## 4. Quy Tắc Xử Lý Mã Lỗi

Xử lý tập trung qua `com.cinema.common.exception.ErrorHandler`:

1. **Chưa đăng nhập hoặc token sai (HTTP 401 Unauthorized):**
   ```json
   {
     "success": false,
     "status": 401,
     "error": "Chua dang nhap"
   }
   ```
2. **Không tìm thấy suất chiếu (HTTP 404 Not Found):**
   ```json
   {
     "success": false,
     "status": 404,
     "error": "Khong tim thay suat chieu"
   }
   ```
3. **Mã suất chiếu không hợp lệ (HTTP 400 Bad Request):**
   ```json
   {
     "success": false,
     "status": 400,
     "error": "Mã suất chiếu 'id' không hợp lệ: abc"
   }
   ```

---

## 5. Kỹ Thuật Truy Vấn CSDL (`ShowtimeDAO.java`)

Truy vấn Native SQL kết hợp giữa bảng `cinema.show_time_seats` và `cinema.seats`, tự động tính toán thời gian hết hạn giữ chỗ:

```sql
SELECT 
    sts.id,
    sts.seat_id,
    s.seat_row,
    s.seat_col,
    s.seat_label,
    sts.price,
    CASE 
        WHEN sts.status = 'SELECTED' AND sts.hold_expiration_at IS NOT NULL AND sts.hold_expiration_at < CURRENT_TIMESTAMP THEN 'AVAILABLE'
        ELSE sts.status
    END AS actual_status,
    CASE 
        WHEN sts.status = 'SELECTED' AND sts.hold_expiration_at IS NOT NULL AND sts.hold_expiration_at < CURRENT_TIMESTAMP THEN NULL
        ELSE CAST(sts.hold_expiration_at AS TEXT)
    END AS actual_hold_expiration_at
FROM cinema.show_time_seats sts
JOIN cinema.seats s ON sts.seat_id = s.seat_id
WHERE sts.show_time_id = :showTimeId
ORDER BY s.seat_row ASC, s.seat_col ASC;
```

---

## 6. Kiểm Thử Đơn Vị (Unit Testing)

Tất cả các kịch bản nghiệp vụ của module `showtime` được kiểm thử tự động trong `ShowtimeServiceTest.java`:

- **Tổng số tests:** 8 test cases (Toàn bộ dự án: 33 test cases).
- **Tỷ lệ vượt qua:** 100% PASS (`BUILD SUCCESS`).
- **Phạm vi kiểm thử:**
  1. `testGetSeatMap_Success`: Lấy sơ đồ ghế thành công, kiểm tra đủ `showTimeId`, danh sách ghế và serialization JSON.
  2. `testGetSeatMap_SeatBooked`: Ghế đã thanh toán (`status = BOOKED`, `holdExpirationAt = null`).
  3. `testGetSeatMap_SeatSelected_ActiveHold`: Ghế đang giữ chỗ còn hạn (`status = SELECTED`, có `holdExpirationAt`).
  4. `testGetSeatMap_ShowtimeNotFound`: Suất chiếu không tồn tại $\rightarrow$ ném 404 (`Khong tim thay suat chieu`).
  5. `testGetSeatMap_InvalidShowtimeId`: Bắt lỗi 400 khi `id` null, rỗng, âm, số 0 hoặc ký tự chữ.
  6. `testGetShowtimeDetail_Success`: Lấy chi tiết suất chiếu thành công (200 OK).
  7. `testGetShowtimeDetail_NotFound`: Suất chiếu không tồn tại (404 Not Found).
  8. `testGetShowtimeDetail_InvalidId`: Bắt lỗi 400 cho `getShowtimeDetail`.
