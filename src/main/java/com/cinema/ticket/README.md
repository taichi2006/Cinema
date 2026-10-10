# 📋 Kế Hoạch Triển Khai Module `ticket`

> **Tài liệu tham chiếu chuẩn**:
> - [cinema sql.txt]
> - [swagger_cinema.yaml]
> - [AuthFilter.java]

---

## 1. Phân Tích CSDL (`cinema sql.txt`)

Bảng `cinema.tickets` lưu trữ vé điện tử gắn liền với đơn đặt vé (`booking_id`) và ghế trong suất chiếu (`show_time_seat_id`):

```sql
CREATE TABLE tickets (
    ticket_id             SERIAL PRIMARY KEY,
    booking_id            INT NOT NULL,
    show_time_seat_id     INT NOT NULL UNIQUE,
    ticket_code           VARCHAR(500) UNIQUE NOT NULL,
    status                VARCHAR(20) DEFAULT 'VALID' CHECK (status IN ('CANCELLED','USED','VALID','REFUNDED')),
    movie_name            VARCHAR(255),
    show_time_start_time  TIMESTAMP,
    room_name             VARCHAR(100),
    seat_name             VARCHAR(20),
    price                 DECIMAL(10,2),
    used_at               TIMESTAMP,
    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE RESTRICT,
    FOREIGN KEY (show_time_seat_id) REFERENCES show_time_seats(id) ON DELETE RESTRICT
);
```

### Chi tiết các thuộc tính:
- `ticket_id`: `INT` (Khóa chính tự tăng).
- `booking_id`: `INT` (Khóa ngoại trỏ đến bảng `bookings`).
- `show_time_seat_id`: `INT` (Khóa ngoại 1-1 trỏ đến `show_time_seats`).
- `ticket_code`: `VARCHAR(500)` (Mã vé duy nhất, sinh ra dưới dạng UUID hoặc chuỗi mã hóa vé điện tử, dùng để quét QR).
- `status`: `VARCHAR(20)` với 4 trạng thái: `VALID`, `USED`, `CANCELLED`, `REFUNDED`.
- `movie_name`: `VARCHAR(255)` (Tên phim chụp nhanh tại thời điểm in vé).
- `show_time_start_time`: `TIMESTAMP` (Thời gian bắt đầu suất chiếu).
- `room_name`: `VARCHAR(100)` (Tên phòng chiếu).
- `seat_name`: `VARCHAR(20)` (Tên ghế, ví dụ: "A1", "H12").
- `price`: `DECIMAL(10,2)` (Giá vé).
- `used_at`: `TIMESTAMP` (Thời điểm vé được soát/sử dụng).
- `created_at`: `TIMESTAMP` (Thời điểm vé được tạo).

---

## 2. Đặc Tả Endpoint Theo Swagger (`swagger_cinema.yaml`)

Module `Ticket` có **2 endpoints** yêu cầu xác thực JWT (`BearerAuth`):

### 2.1. `GET /bookings/{bookingId}/tickets` — Danh sách vé của Booking
- **Mô tả**: Lấy danh sách tất cả các vé của một booking cụ thể.
- **Xác thực**: Yêu cầu đăng nhập (`userId` lấy từ JWT trong `AuthFilter`).
- **Phân quyền / Kiểm tra bảo mật**:
  - Kiểm tra xem booking có tồn tại không. Nếu không -> `404 Not Found`.
  - Kiểm tra booking đó có thuộc về `userId` đang đăng nhập không. Nếu không -> `403 Forbidden` (hoặc `404 Not Found` để bảo mật).
- **Parameter**:
    - `bookingId`: `integer` (path).
- **Responses**:
  - `200 OK`: Trả về mảng vé `Ticket[]`.
    ```json
    {
      "success": true,
      "data": [
        {
          "ticketId": 9001,
          "bookingId": 1001,
          "showTimeSeatId": 5001,
          "ticketCode": "TKT-A1B2-C3D4",
          "status": "VALID",
          "movieName": "Inception",
          "showTimeStartTime": "2024-07-15T19:00:00Z",
          "roomName": "Room 1",
          "seatName": "A1",
          "price": 90000,
          "usedAt": null
        }
      ]
    }
    ```
  - `401 Unauthorized`: Chưa đăng nhập / Token không hợp lệ.
    ```json
    {
        "success": false,
        "status": 401,
        "error": "Chua dang nhap"
    }
     ```
  - `404 Not Found`: Không tìm thấy booking.
    ```json
    {
      "success": false,
      "status": 404,
      "error": "Booking khong ton tai"
    }
    ```

---

### 2.2. `GET /tickets/{ticketId}` — Chi tiết vé
- **Mô tả**: Xem thông tin chi tiết của 1 chiếc vé theo ID.
- **Xác thực**: Yêu cầu đăng nhập.
- **Phân quyền / Kiểm tra bảo mật**:
  - Vé phải tồn tại trong hệ thống.
  - User sở hữu booking chứa vé này (hoặc Admin) mới có quyền xem thông tin vé.
- **Parameter**:
    - `ticketId`: `integer` (path).
- **Responses**:
  - `200 OK`: Trả về đối tượng `Ticket`.
    ```json
    {
      "success": true,
      "data": {
        "ticketId": 9001,
        "bookingId": 1001,
        "showTimeSeatId": 5001,
        "ticketCode": "TKT-A1B2-C3D4",
        "status": "VALID",
        "movieName": "Inception",
        "showTimeStartTime": "2024-07-15T19:00:00Z",
        "roomName": "Room 1",
        "seatName": "A1",
        "price": 90000,
        "usedAt": null
      }
    }
    ```
  - `401 Unauthorized`: Chưa đăng nhập.
    ```json
    {
        "success": false,
        "status": 401,
        "error": "Chua dang nhap"
    }
     ```
  - `404 Not Found`: Không tìm thấy vé.
    ```json
    {
      "success": false,
      "status": 404,
      "error": "Ve khong ton tai"
    }
    ```

---

## 3. Cấu Trúc Thiết Kế Module `ticket`

Tuân thủ cấu trúc phẳng trong package `com.cinema.ticket`:

```
src/main/java/com/cinema/ticket/
├── Ticket.java                ← Entity JPA ánh xạ bảng cinema.tickets
├── TicketStatus.java          ← Enum: VALID, USED, CANCELLED, REFUNDED
├── TicketResponse.java        ← DTO biểu diễn dữ liệu vé theo Swagger
├── TicketDAO.java             ← Truy vấn JPA JPQL cho tickets (và kiểm tra quan hệ booking)
├── TicketService.java         ← Nghiệp vụ: lấy vé theo booking, lấy chi tiết vé, xác thực quyền sở hữu
├── TicketController.java      ← Servlet ánh xạ /tickets, /tickets/*
├── TicketException.java       ← Ngoại lệ nghiệp vụ riêng cho Ticket
└── README.md                  ← Tài liệu này
```

> **Định tuyến URL Servlet (Phương án 1 - Xử lý tập trung tại `TicketController`)**:
> - Khai báo ánh xạ: `@WebServlet(name = "TicketController", urlPatterns = {"/tickets", "/tickets/*", "/bookings/*"})`.
> - Cơ chế phân tích path:
>   - Nếu request dạng `GET /bookings/{bookingId}/tickets`: trích xuất `bookingId` và trả về danh sách vé của booking đó.
>   - Nếu request dạng `GET /tickets/{ticketId}`: trích xuất `ticketId` và trả về chi tiết vé đó.
> - **Bảo mật**:
>   - Các đường dẫn `/bookings/*` đã được [AuthFilter.java] chặn và xác thực tự động.
>   - Đăng ký bổ sung pattern `/tickets`, `/tickets/*`, `/api/tickets`, `/api/tickets/*` vào [AuthFilter.java] để bảo vệ cả endpoint xem chi tiết vé.

---

## 4. Các Bước Triển Khai

1. **Bước 1**: Tạo enum `TicketStatus.java` và Entity `Ticket.java`.
2. **Bước 2**: Đăng ký Entity `com.cinema.ticket.Ticket` vào [persistence.xml](file:///d:/IT1_UTE/HK5_26_27/lap_trinh_web/project/cinema_system/src/main/resources/META-INF/persistence.xml).
3. **Bước 3**: Tạo `TicketResponse.java` và `TicketException.java`.
4. **Bước 4**: Tạo `TicketDAO.java` hỗ trợ tìm vé theo `bookingId`, tìm theo `ticketId`, và kiểm tra `user_id` của booking.
5. **Bước 5**: Tạo `TicketService.java` xử lý logic bảo mật & chuyển đổi DTO.
6. **Bước 6**: Tạo `TicketController.java` xử lý request `GET /tickets/{ticketId}` và `GET /bookings/{bookingId}/tickets`.
7. **Bước 7**: Cập nhật [AuthFilter.java](file:///d:/IT1_UTE/HK5_26_27/lap_trinh_web/project/cinema_system/src/main/java/com/cinema/common/filter/AuthFilter.java) thêm URL pattern cho `/tickets/*` nếu cần.
8. **Bước 8**: Viết Unit Test `TicketModuleTest.java`, chạy `mvnw test` để đảm bảo 100% test cases thành công.

---

## 5. Chỉnh sửa ngoài module
- Thêm endpoint GET /bookings/{bookingId}/tickets vào [AuthFilter.java]