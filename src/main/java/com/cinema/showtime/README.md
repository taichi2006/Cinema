# Tài Liệu Kỹ Thuật: Module Showtime (`com.cinema.showtime`)

Tài liệu kỹ thuật mô tả kiến trúc, endpoint API, cấu trúc sơ đồ ghế, trạng thái đặt chỗ theo thời gian thực và giải pháp tối ưu truy vấn CSDL của module suất chiếu & sơ đồ ghế.

---

## 1. Cấu Trúc Thành Phần

```text
com.cinema.showtime/
├── ShowtimeController.java                  # Servlet tiếp nhận HTTP GET /showtime/{id}/seat & xác thực BearerAuth
├── ShowtimeService.java                     # Xử lý validation (400, 404, 422) và mapping dữ liệu SeatMap
├── ShowtimeDAO.java                         # Tầng truy vấn CSDL Native SQL tối ưu
├── DTO/
│   └── Response/
│       ├── ShowtimeDetailResponse.java      # DTO 14 trường thông tin suất chiếu
│       ├── SeatResponse.java                # DTO 10 trường thông tin chi tiết từng ghế
│       └── SeatMapResponse.java             # DTO envelope chứa showtime, serverTime, screenPosition, seats
└── README.md                                # Tài liệu kỹ thuật module
```

---

## 2. Đặc Tả API: Sơ Đồ Và Tình Trạng Ghế

- **Endpoint:** `GET /api/showtime/{id}/seat`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/showtime", "/showtime/*"})` (chuyển tiếp qua `ApiPrefixFilter`)
- **Quyền truy cập:** Bắt buộc đăng nhập (`BearerAuth`), truyền qua header `Authorization: Bearer <accessToken>`
- **Content-Type:** `application/json;charset=UTF-8`
- **Mục đích:** Khách hàng xem sơ đồ ghế và tình trạng ghế theo thời gian thực (read-only, **GET không giữ ghế**).

### 2.1. Tham Số Đầu Vào

| Tham số | Vị trí | Kiểu | Bắt buộc | Ràng buộc kỹ thuật |
| :--- | :--- | :--- | :---: | :--- |
| `id` | Path | `String` / `Long` | **Có** | Mã suất chiếu (`showtime_id`). Phải là số nguyên dương $\ge 1$. Sai kiểu hoặc âm $\rightarrow$ lỗi 400. |
| `Authorization` | Header | `String` | **Có** | Header chứa `Bearer <jwt_token>`. Thiếu header hoặc token không hợp lệ / hết hạn $\rightarrow$ lỗi 401. |

### 2.2. Phản Hồi Thành Công (HTTP 200 OK)

Cấu trúc flat JSON chuẩn hóa với `meta: {}`:

```json
{
  "success": true,
  "data": {
    "showtime": {
      "id": "101",
      "movieId": "10",
      "movieTitle": "Dune: Part Two",
      "cinemaId": "1",
      "cinemaName": "Galaxy Nguyễn Du",
      "roomId": "5",
      "roomName": "Cinema 1",
      "startsAt": "2026-10-05T18:00:00+07:00",
      "endsAt": "2026-10-05T20:30:00+07:00",
      "format": "2D",
      "language": "VI",
      "minTicketPrice": 85000,
      "currency": "VND",
      "availableSeatCount": 95
    },
    "serverTime": "2026-10-05T11:00:00.000Z",
    "screenPosition": "TOP",
    "seats": [
      {
        "id": "501",
        "row": "A",
        "number": 1,
        "x": 0,
        "y": 0,
        "type": "STANDARD",
        "price": 85000,
        "status": "AVAILABLE",
        "heldByCurrentUser": false,
        "holdExpiresAt": null
      },
      {
        "id": "502",
        "row": "A",
        "number": 2,
        "x": 1,
        "y": 0,
        "type": "STANDARD",
        "price": 85000,
        "status": "HELD",
        "heldByCurrentUser": true,
        "holdExpiresAt": "2026-10-05T11:05:00.000Z"
      }
    ]
  },
  "meta": {}
}
```

### 2.3. Chi Tiết Dữ Liệu Phản Hồi

1. **`showtime` (14 trường thông tin suất chiếu):**
   - `id`: Mã suất chiếu (`showtime_id`).
   - `movieId`, `movieTitle`: Mã và tên phim.
   - `cinemaId`, `cinemaName`: Mã và tên cụm rạp.
   - `roomId`, `roomName`: Mã và tên phòng chiếu.
   - `startsAt`, `endsAt`: Thời gian bắt đầu và kết thúc (định dạng ISO-8601).
   - `format`: Định dạng chiếu (`2D`, `3D`, `IMAX`).
   - `language`: Ngôn ngữ / phụ đề (`VI`, `VietSub`, `Dub`).
   - `minTicketPrice`: Giá vé thấp nhất khả dụng của suất chiếu (`Long`).
   - `currency`: Đơn vị tiền tệ (`"VND"`).
   - `availableSeatCount`: Số ghế còn khả dụng (`Integer`).
2. **`serverTime`:** Thời điểm hiện tại của máy chủ theo chuẩn ISO-8601 UTC để đồng bộ đồng hồ đếm ngược giữ ghế phía giao diện người dùng.
3. **`screenPosition`:** Vị trí màn hình chiếu (`TOP` hoặc `BOTTOM`), lấy từ `cinema.rooms.screen_position`.
4. **`seats` (Mảng thông tin chi tiết từng ghế):**
   - `id`: Mã ghế (`seat_id`).
   - `row`: Hàng ghế (`seat_row`, ví dụ: `"A"`, `"B"`).
   - `number`: Số thứ tự ghế trong hàng (`seat_number`).
   - `x`, `y`: Tọa độ vị trí ghế trên sơ đồ 2D.
   - `type`: Tên loại ghế (`seat_types.type_name`, ví dụ: `"STANDARD"`, `"VIP"`, `"COUPLE"`).
   - `price`: Giá vé của ghế (lấy từ cấu hình riêng `showtime_seats.price`, fallback về `s.base_price + st.extra_price`).
   - `status`: Trạng thái ghế tại thời điểm gọi API:
     - `"BLOCKED"`: Ghế bị khóa bảo trì (`seats.status = 'BLOCKED'` hoặc `showtime_seats.is_blocked = true`).
     - `"BOOKED"`: Ghế đã được đặt và thanh toán thành công (`booking_seats.status = 'BOOKED'` hoặc `bookings.status = 'PAID'`).
     - `"HELD"`: Ghế đang được giữ chỗ trong phiên đặt vé hợp lệ (`booking_seats.status = 'HELD'` và `bookings.status = 'PENDING_PAYMENT'` và `bookings.expires_at > CURRENT_TIMESTAMP`).
     - `"AVAILABLE"`: Ghế trống sẵn sàng để đặt.
   - `heldByCurrentUser`: Cờ `true` nếu ghế này đang được giữ bởi chính tài khoản đang gửi request (`booking.user_id = currentUserId`), ngược lại là `false`.
   - `holdExpiresAt`: Thời điểm hết hạn giữ ghế (ISO-8601) nếu ghế đang được giữ (`bookings.expires_at`), ngược lại là `null`.

---

## 3. Quy Tắc Xử Lý Mã Lỗi

Xử lý tập trung qua `com.cinema.common.exception.ErrorHandler`:

1. **Chưa đăng nhập hoặc token sai (HTTP 401 Unauthorized):**
   ```json
   {
     "success": false,
     "status": 401,
     "error": "Chưa đăng nhập hoặc phiên làm việc đã hết hạn"
   }
   ```
2. **Không tìm thấy suất chiếu (HTTP 404 Not Found - `SHOWTIME_NOT_FOUND`):**
   ```json
   {
     "success": false,
     "status": 404,
     "error": "Không tìm thấy suất chiếu"
   }
   ```
3. **Suất chiếu đã bắt đầu hoặc đóng bán (HTTP 422 Unprocessable Entity - `SHOWTIME_NOT_BOOKABLE`):**
   - Kích hoạt khi suất chiếu có `status != 'OPEN'` (ví dụ: `CLOSED`, `CANCELLED`, `ENDED`).
   - Kích hoạt khi thời gian bắt đầu `starts_at <= CURRENT_TIMESTAMP`.
   ```json
   {
     "success": false,
     "status": 422,
     "error": "Suất chiếu đã bắt đầu hoặc đã đóng bán"
   }
   ```
4. **Mã suất chiếu không hợp lệ (HTTP 400 Bad Request):**
   ```json
   {
     "success": false,
     "status": 400,
     "error": "Mã suất chiếu 'id' không hợp lệ: abc"
   }
   ```

---

## 4. Kỹ Thuật Truy Vấn CSDL (`ShowtimeDAO.java`)

Sử dụng câu truy vấn Native SQL kết hợp tận dụng Partial Unique Index trên PostgreSQL:

```sql
SELECT 
    se.seat_id,
    se.seat_row,
    se.seat_number,
    se.x,
    se.y,
    st.type_name,
    COALESCE(ss.price, s.base_price + st.extra_price) AS price,
    CASE 
        WHEN se.status = 'BLOCKED' OR COALESCE(ss.is_blocked, false) = true THEN 'BLOCKED'
        WHEN bs.status = 'BOOKED' OR b.status = 'PAID' THEN 'BOOKED'
        WHEN bs.status = 'HELD' AND b.status = 'PENDING_PAYMENT' AND b.expires_at > CURRENT_TIMESTAMP THEN 'HELD'
        ELSE 'AVAILABLE'
    END AS seat_status,
    CASE 
        WHEN bs.status = 'HELD' AND b.status = 'PENDING_PAYMENT' AND b.expires_at > CURRENT_TIMESTAMP AND b.user_id = :currentUserId THEN true
        ELSE false
    END AS held_by_current_user,
    CASE 
        WHEN bs.status = 'HELD' AND b.status = 'PENDING_PAYMENT' AND b.expires_at > CURRENT_TIMESTAMP THEN b.expires_at
        ELSE NULL
    END AS hold_expires_at
FROM cinema.seats se
JOIN cinema.rooms r ON se.room_id = r.room_id
JOIN cinema.seat_types st ON se.seat_type_id = st.seat_type_id
JOIN cinema.showtimes s ON s.showtime_id = :showtimeId AND s.room_id = r.room_id
LEFT JOIN cinema.showtime_seats ss ON ss.showtime_id = s.showtime_id AND ss.seat_id = se.seat_id
LEFT JOIN cinema.booking_seats bs ON bs.showtime_id = s.showtime_id AND bs.seat_id = se.seat_id AND bs.status IN ('HELD', 'BOOKED')
LEFT JOIN cinema.bookings b ON bs.booking_id = b.booking_id AND b.status IN ('PENDING_PAYMENT', 'PAID')
WHERE se.room_id = :roomId
ORDER BY se.y ASC, se.x ASC, se.seat_id ASC;
```

### Ưu điểm kỹ thuật:
- **Ngăn chặn tích Cartesian (Cartesian Product):** Nhờ Partial Unique Index `uq_active_seat_reservation (showtime_id, seat_id)` trên `booking_seats`, mỗi ghế tại một suất chiếu chỉ nối tối đa 1 dòng active reservation, đảm bảo tốc độ truy vấn tối ưu và dữ liệu không bị nhân đôi.
- **Sắp xếp trực quan:** Tọa độ sắp xếp theo `se.y ASC, se.x ASC` giúp client dễ dàng vẽ lại layout sơ đồ phòng chiếu theo hàng và cột.

---

## 5. Kiểm Thử Đơn Vị (Unit Testing)

Tất cả các kịch bản nghiệp vụ của module `showtime` được kiểm thử tự động trong `ShowtimeServiceTest.java` thông qua kỹ thuật mock DAO (`StubShowtimeDAO`):

- **Tổng số tests:** 9 test cases (Toàn bộ dự án: 55 test cases).
- **Tỷ lệ vượt qua:** 100% PASS (`BUILD SUCCESS`).
- **Phạm vi kiểm thử:**
  1. `testGetSeatMap_Success`: Lấy sơ đồ ghế thành công, kiểm tra đủ cấu trúc `showtime` (14 trường), `serverTime`, `screenPosition`, `seats` và `meta: {}`.
  2. `testGetSeatMap_SeatHeldByCurrentUser`: Ghế do chính user đang giữ có `status = "HELD"` và `heldByCurrentUser = true`, kèm `holdExpiresAt`.
  3. `testGetSeatMap_SeatHeldByAnotherUser`: Ghế do user khác giữ có `status = "HELD"` nhưng `heldByCurrentUser = false`.
  4. `testGetSeatMap_SeatBooked`: Ghế đã thanh toán có `status = "BOOKED"`, `heldByCurrentUser = false`, `holdExpiresAt = null`.
  5. `testGetSeatMap_SeatBlocked`: Ghế bị khóa bảo trì có `status = "BLOCKED"`.
  6. `testGetSeatMap_ShowtimeNotFound`: Suất chiếu không tồn tại $\rightarrow$ ném 404 (`SHOWTIME_NOT_FOUND`).
  7. `testGetSeatMap_ShowtimeNotBookable_StatusNotOpen`: Suất chiếu ở trạng thái `CLOSED` hoặc `CANCELLED` $\rightarrow$ ném 422 (`SHOWTIME_NOT_BOOKABLE`).
  8. `testGetSeatMap_ShowtimeNotBookable_PastStartTime`: Suất chiếu đã bắt đầu trong quá khứ $\rightarrow$ ném 422 (`SHOWTIME_NOT_BOOKABLE`).
  9. `testGetSeatMap_InvalidShowtimeId`: Bắt lỗi 400 khi `id` null, trống, âm, số 0 hoặc chữ cái.
