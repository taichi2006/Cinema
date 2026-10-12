# TÀI LIỆU CƠ CHẾ VẬN HÀNH WEBSOCKET ĐỒNG BỘ GHẾ THỜI GIAN THỰC (REAL-TIME SEAT LOCKING)

---

## 1. TỔNG QUAN & BỐI CẢNH (OVERVIEW)

### 1.1. Vấn đề của mô hình HTTP truyền thống
Trong hệ thống đặt vé xem phim trực tuyến, nếu chỉ sử dụng HTTP Request / Response thông thường:
- Khi Khách hàng A đang chọn ghế `H8`, Khách hàng B cùng xem suất chiếu đó sẽ **không hề hay biết** ghế `H8` đang có người chọn.
- Khách hàng B vẫn bấm chọn được ghế `H8`. Đến bước thanh toán cuối cùng, hệ thống mới báo lỗi *"Ghế đã bị đặt"*, gây ức chế và trải nghiệm người dùng kém (Bad UX).

### 1.2. Giải pháp: WebSocket Real-time
Triển khai giao thức **WebSocket hai chiều (Full-Duplex)**:
- Kết nối được duy trì liên tục giữa Client và Server.
- Ngay khi Khách hàng A bấm chọn ghế `H8`, Server lập tức phát sóng (broadcast) trạng thái tới toàn bộ các khách hàng khác đang xem cùng suất chiếu đó.
- Ghế `H8` trên màn hình của Khách hàng B lập tức chuyển sang màu **Xám (Đang có người giữ)** và bị vô hiệu hóa (`disabled`) trong thời gian thực (< 100ms).

---

## 2. SƠ ĐỒ KIẾN TRÚC TỔNG THỂ (ARCHITECTURE)

```text
┌─────────────────────────────────┐                 ┌─────────────────────────────────┐
│     KHÁCH HÀNG A (Browser 1)    │                 │     KHÁCH HÀNG B (Browser 2)    │
│  • Bấm chọn ghế H8 (Seat ID 21) │                 │  • Đang xem cùng suất chiếu 105 │
└────────────────┬────────────────┘                 └────────────────▲────────────────┘
                 │ 1. Gửi HOLD_SEAT                                  │ 4. Nhận SEAT_STATE_CHANGED
                 ▼                                                   │    ➜ Đổi ghế H8 sang MÀU XÁM!
┌────────────────────────────────────────────────────────────────────┴────────────────┐
│                           BACKEND SERVER (CINEMA API)                               │
│                                                                                     │
│  [1. SeatWebSocketEndpoint.java]                                                    │
│      • Tiếp nhận kết nối tại URL: /ws/seats/{showtimeId}?token=...                  │
│      • Giải mã Bearer JWT token lấy userId                                          │
│                                                                                     │
│  [2. SeatSessionManager.java] (Quản lý phòng chiếu & Concurrency)                   │
│      • showtimeRooms: Gom các Session đang cùng xem suất chiếu 105                   │
│      • showtimeHeldSeats: Lưu vết (Seat ID 21 ➜ User A)                             │
│      • Kiểm tra tranh chấp (Ai bấm trước 1 mili-giây thì được giữ)                  │
│      • Broadcast thông báo tới tất cả Session trong phòng                           │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. CƠ CHẾ BẢO MẬT & XÁC THỰC JWT TRÊN WEBSOCKET

Khác với HTTP truyền thống truyền token qua Header `Authorization`, giao thức WebSocket khởi tạo bằng bắt tay HTTP Handshake:
1. **Client gửi yêu cầu kết nối**:
   ```
   wss://be-cinema.onrender.com/ws/seats/105?token=eyJhbGciOiJIUzI1NiIsInR5cCI6...
   ```
2. **Server (`SeatWebSocketEndpoint.java`) thẩm định**:
   - Bóc tách Query Param `token`.
   - Dùng thư viện `JwtUtil.parseAccessToken(token)` để xác minh tính toàn vẹn và hạn sử dụng của token.
   - Trích xuất `userId` của người dùng và lưu vào phiên làm việc (`Session`).
   - Nếu không có token hoặc token giả mạo: Người dùng chỉ có quyền "Xem" trạng thái ghế của người khác, **không có quyền phát lệnh giữ ghế**.

---

## 4. QUY CHUẨN GÓI TIN JSON TRAO ĐỔI (SOCKET PROTOCOL)

Tất cả các gói tin trao đổi qua Socket đều tuân thủ cấu trúc đối tượng `SeatSocketMessage`:

### 4.1. Khách hàng bấm chọn ghế (Client ➜ Server)
```json
{
  "type": "HOLD_SEAT",
  "seatId": 21
}
```

### 4.2. Khách hàng bấm bỏ chọn ghế (Client ➜ Server)
```json
{
  "type": "RELEASE_SEAT",
  "seatId": 21
}
```

### 4.3. Server phát sóng trạng thái ghế thay đổi (Server ➜ Mọi Client trong phòng)
```json
{
  "type": "SEAT_STATE_CHANGED",
  "showtimeId": 105,
  "seatId": 21,
  "userId": 15,
  "status": "HOLDING",
  "timestamp": 1790863247000
}
```
* **Tại Client:** 
  - Nếu `userId === currentUserId`: Ghế đổi sang màu **Cam / Xanh** (*"Ghế bạn đang chọn"*).
  - Nếu `userId !== currentUserId`: Ghế đổi sang màu **Xám** (*"Đang có người khác giữ"*), gắn tooltip cảnh báo và khóa click (`disabled`).

### 4.4. Trạng thái khởi tạo khi người mới vừa vào phòng (Server ➜ Client mới)
Khi một khách hàng vừa mở trang sơ đồ ghế, Server gửi ngay toàn bộ các ghế đang bị người khác giữ tạm thời:
```json
{
  "type": "INITIAL_STATE",
  "showtimeId": 105,
  "holdingSeats": {
    "21": 15,
    "22": 15
  }
}
```

### 4.5. Giữ kết nối sống (Keep-Alive Ping/Pong)
- Định kỳ mỗi 25 giây, Frontend gửi gói tin: `{"type": "PING"}`.
- Server phản hồi ngay: `{"type": "PONG"}` để duy trì kết nối qua các tầng Router/Proxy không bị ngắt.

---

## 5. CƠ CHẾ XỬ LÝ ĐỒNG THỜI & CHỐNG TRANH CHẤP (CONCURRENCY)

Nếu **2 người dùng cùng click vào ghế H8 ở cùng một tích tắc (mili-giây)**:
1. `SeatSessionManager` sử dụng `ConcurrentHashMap` an toàn luồng (Thread-safe) trong bộ nhớ:
   ```java
   Long currentHolder = heldSeats.get(seatId);
   if (currentHolder != null && !Objects.equals(currentHolder, userId)) {
       // Ghế đã có người nhanh tay hơn giữ trước!
       sendDirect(session, SeatSocketMessage.error("Ghế đã có người khác chọn trước!"));
       return false;
   }
   heldSeats.put(seatId, userId);
   ```
2. Người nào gói tin chạm Server trước 1 phần nghìn giây sẽ được quyền giữ ghế.
3. Người đến sau ngay lập tức nhận gói tin lỗi `ERROR`, ghế lập tức xám đi trên màn hình của họ, đảm bảo không bao giờ xảy ra tình trạng 2 người giữ trùng 1 ghế.

---

## 6. CƠ CHẾ VÒNG ĐỜI GHẾ & TỰ ĐỘNG DỌN DẸP (LIFECYCLE & FAULT TOLERANCE)

### 6.1. Phân biệt "Rời khỏi do Hủy/Tắt Tab" vs "Rời khỏi do Chuyển sang Đặt vé"
* **Trường hợp A: Khách hàng chỉ chọn ghế rồi tắt tab hoặc bấm quay lại (Abandon):**
  - Khi ngắt kết nối, Server kiểm tra các ghế này **chưa được tạo đơn**.
  - Server tự động **nhả các ghế về trạng thái `AVAILABLE`** và phát sóng cho toàn phòng chiếu để người khác vào đặt.
* **Trường hợp B: Khách hàng bấm "Tiếp tục (Đặt vé)" (Proceed to Booking):**
  - API `POST /bookings` tạo đơn thành công, Server gọi `SeatSessionManager.markBooked(...)`.
  - Các ghế này được chuyển từ danh sách giữ tạm sang danh sách **`showtimeBookedSeats`**.
  - Server phát sóng `SEAT_STATE_CHANGED(seatId, "BOOKED")`.
  - Khi Khách hàng chuyển sang trang đơn hàng `/booking/{id}` (kết nối socket chọn ghế đóng lại), Server **KHÔNG NHẢ GHẾ**, ghế tiếp tục được khóa an toàn trong 10 phút.

### 6.2. Tự động nhả ghế thời gian thực khi Hủy vé (`CANCEL`) hoặc Quá hạn 10 phút (`EXPIRED`)
* **Khách hàng chủ động hủy vé (`POST /bookings/{id}/cancel`):**
  - CSDL cập nhật ghế về `AVAILABLE`.
  - `BookingDAO` tự động kích hoạt `SeatSessionManager.releaseBookedSeat(...)`.
  - Toàn bộ các khách hàng đang ở màn hình chọn ghế lập tức thấy ghế sáng trở lại (AVAILABLE) mà **không cần phải F5**.
* **Đơn hàng quá hạn giữ 10 phút (`EXPIRED`):**
  - Hệ thống tự động giải phóng ghế và phát sóng `AVAILABLE` tức thì qua WebSocket cho toàn bộ phòng chiếu.

---

## 7. CẤU TRÚC MÃ NGUỒN ĐÃ TRIỂN KHAI

### 7.1. Phía Backend (`Cinema_api`):
* `src/main/java/com/cinema/websocket/SeatSocketMessage.java`: DTO chuẩn hóa gói tin.
* `src/main/java/com/cinema/websocket/SeatSessionManager.java`: Quản lý Room, Cache ghế và phát sóng.
* `src/main/java/com/cinema/websocket/SeatWebSocketEndpoint.java`: Điểm tiếp nhận kết nối `/ws/seats/{showtimeId}`.
* `pom.xml`: Bổ sung `jakarta.websocket-api` & `jakarta.websocket-client-api`.

### 7.2. Phía Frontend (`web_final`):
* `src/websocket/websocketConfig.js`: Tự động chuyển đổi URL `ws://` (Local) và `wss://` (Production).
* `src/websocket/useSeatWebSocket.js`: Custom Hook quản lý vòng đời socket, tự động Reconnect và Keep-alive.
* `src/screens/SeatSelectionScreen.jsx`: Tích hợp hiển thị ghế xám realtime và xử lý chọn/nhả ghế.
* `src/index.css`: Bổ sung style class `.seat-cell.holding-by-other` (màu xám viền dashed) và Legend chú thích.

---

## 8. HƯỚNG DẪN THỬ NGHIỆM THỰC TẾ (DEMO GUIDE)

Để kiểm chứng tính năng thời gian thực hoạt động:
1. Mở 2 cửa sổ trình duyệt:
   - Cửa sổ 1: Trình duyệt bình thường (đăng nhập tài khoản A).
   - Cửa sổ 2: Trình duyệt ẩn danh Incognito (đăng nhập tài khoản B).
2. Cả 2 cửa sổ cùng vào màn hình chọn ghế của **cùng 1 suất chiếu**.
3. **Tại Cửa sổ 1:** Click chọn một ghế bất kỳ (ví dụ ghế `D5`).
4. **Quan sát tại Cửa sổ 2:** Ghế `D5` lập tức chuyển sang **MÀU XÁM**, bị khóa click và hiển thị tooltip *"Đang có người khác giữ chỗ (Thời gian thực)"*.
5. **Tại Cửa sổ 1:** Click bỏ chọn ghế `D5`.
6. **Quan sát tại Cửa sổ 2:** Ghế `D5` lập tức **sáng trở lại màu bình thường** và cho phép Cửa sổ 2 bấm chọn!
7. **Kịch bản Tiếp tục đặt vé (Proceed to Checkout):**
   - Tại Cửa sổ 1: Chọn ghế `D5`, bấm **TIẾP TỤC (ĐẶT VÉ)** để sang trang thanh toán `/booking/{id}`.
   - Tại Cửa sổ 2: Ghế `D5` lập tức chuyển sang trạng thái **ĐÃ CÓ NGƯỜI ĐẶT (ĐEN/XÁM ĐẬM)**. Cửa sổ 2 chọn bất kỳ ghế nào khác cũng không thể tác động hay mở khóa ghế `D5`.
8. **Kịch bản Đổi ghế hoặc Hủy vé từ trang thanh toán:**
   - Tại Cửa sổ 1 (đang ở trang thanh toán `/booking/{id}`): Bấm nút **"Đổi lại ghế"** hoặc **"Hủy đơn đặt vé & Đổi ghế khác"** $\rightarrow$ Xác nhận *"Đồng ý & Chọn lại ghế"*.
   - Đơn vé được hủy, Cửa sổ 1 quay lại sơ đồ chọn ghế.
   - Tại Cửa sổ 2: Ghế `D5` lập tức **giải phóng thành AVAILABLE (sáng màu trở lại)** trong thời gian thực mà **không cần tải lại trang (F5)**!
