# 🎬 CINEMA BACKEND - TÀI LIỆU TOÀN BỘ API ENDPOINTS

Tài liệu chuẩn xác 100% theo mã nguồn thực tế của hệ thống Cinema Backend (Jakarta EE / Servlet + JPA).

---

## 🌐 1. BASE URL

* **Môi trường Cloud (Render)**:  
  `https://be-cinema.onrender.com`
* **Môi trường Local (Tomcat / IntelliJ)**:  
  `http://localhost:8080/cinema_war_exploded`

> [!TIP]
> Tất cả API trong code được map trực tiếp theo các Servlet gốc: `/auth`, `/movies`, `/bookings`, `/wallet`, `/cinemas`...  
> *(Hệ thống có bộ lọc `ApiPrefixFilter` hỗ trợ thêm nếu bạn muốn gọi qua prefix `/api/` hoặc `/api/v1/`)*.

---

## 🔐 2. CƠ CHẾ XÁC THỰC (AUTHENTICATION)

Hệ thống sử dụng **JWT (JSON Web Token)**:
* **Header**: `Authorization: Bearer <access_token>`
* **Cookie**: Nhận diện tự động qua cookie `access_token` nếu không truyền header.
* **Các route yêu cầu đăng nhập (🔒)**: `/user`, `/user/*`, `/wallet`, `/wallet/*`, `/bookings`, `/bookings/*`, `/tickets`, `/tickets/*`, `/auth/logout`, `/auth/change-password`, `POST /movies/{id}/reviews`.

---

## 📦 3. ĐỊNH DẠNG PHẢN HỒI CHUẨN (RESPONSE FORMAT)

### 3.1. Phản hồi thành công (`ApiResponse`)
```json
{
  "code": 200,
  "message": "OK",
  "data": { ... }
}
```

### 3.2. Phản hồi danh sách phân trang (`PageResponse`)
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "items": [ ... ],
    "meta": {
      "page": 0,
      "size": 20,
      "totalElements": 100,
      "totalPages": 5
    }
  }
}
```

### 3.3. Phản hồi lỗi (`ErrorResponse`)
```json
{
  "code": 400,
  "message": "Thông báo lỗi chi tiết"
}
```

---

## 📋 4. DANH SÁCH CHI TIẾT TẤT CẢ ENDPOINTS THEO CODE

---

### 🟢 MODULE 1: AUTHENTICATION (`/auth`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `POST` | `/auth/register` | ❌ Không | Đăng ký tài khoản người dùng mới (Status `201 Created`) |
| `POST` | `/auth/login` | ❌ Không | Đăng nhập tài khoản (trả về Access & Refresh Token, set Cookie) |
| `POST` | `/auth/logout` | 🔒 Có / Optional | Đăng xuất (xóa cookie và thu hồi token) |
| `POST` | `/auth/refresh` | ❌ Không | Cấp mới Access Token bằng Refresh Token (qua Cookie hoặc Body) |
| `POST` | `/auth/change-password` | 🔒 Có | Đổi mật khẩu tài khoản đang đăng nhập |

#### Request Body mẫu:
* **POST `/auth/register`**:
  ```json
  {
    "fullName": "Nguyễn Văn A",
    "email": "user@example.com",
    "password": "Password123@",
    "phone": "0987654321",
    "dob": "2000-01-15"
  }
  ```
* **POST `/auth/login`**:
  ```json
  {
    "email": "user@example.com",
    "password": "Password123@"
  }
  ```
* **POST `/auth/refresh`**:
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```
* **POST `/auth/change-password`**:
  ```json
  {
    "oldPassword": "Password123@",
    "newPassword": "NewPassword456@"
  }
  ```

---

### 🟢 MODULE 2: TRANG CHỦ (`/home`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/home` | ❌ Không | Lấy dữ liệu trang chủ: banners, phim đang chiếu, phim sắp chiếu, tin tức hot |

---

### 🟢 MODULE 3: PHIM & ĐÁNH GIÁ (`/movies`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/movies` | ❌ Không | Danh sách phim (tìm kiếm, lọc thể loại, trạng thái, phân trang) |
| `GET` | `/movies/{id}` | ❌ Không | Chi tiết thông tin phim theo ID |
| `GET` | `/movies/{id}/showtimes` | ❌ Không | Lịch chiếu của phim theo ngày |
| `GET` | `/movies/{id}/reviews` | ❌ Không | Danh sách đánh giá & bình luận của phim |
| `POST` | `/movies/{id}/reviews` | 🔒 Có | Gửi đánh giá cho phim (1 - 5 sao) |

#### Tham số Query:
* **GET `/movies`**:
  * `q`: Từ khóa tìm kiếm tên phim
  * `genre`: Mã thể loại (ví dụ: `ACTION`, `COMEDY`)
  * `status`: Trạng thái (`NOW_SHOWING`, `COMING_SOON`)
  * `page`: Trang (bắt đầu từ 0)
  * `size`: Số bản ghi mỗi trang (mặc định 20)
  * `sort`: Sắp xếp (`releaseDate,desc`, `title,asc`, ...)
* **GET `/movies/{id}/showtimes`**:
  * `date` *(bắt buộc)*: Ngày chiếu (`YYYY-MM-DD`, ví dụ: `2026-10-15`)
  * `cinemaId` *(tùy chọn)*: Lọc theo rạp
  * `page`, `size`
* **POST `/movies/{id}/reviews`** (Body):
  ```json
  {
    "rating": 5,
    "comment": "Phim hay, kỹ xảo đỉnh cao!"
  }
  ```

---

### 🟢 MODULE 4: RẠP CHIẾU (`/cinemas`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/cinemas` | ❌ Không | Danh sách rạp chiếu (lọc theo thành phố, tìm kiếm) |
| `GET` | `/cinemas/{id}` | ❌ Không | Chi tiết thông tin rạp chiếu theo ID |
| `GET` | `/cinemas/{id}/showtimes` | ❌ Không | Lịch chiếu phim tại rạp theo ngày |

#### Tham số Query:
* **GET `/cinemas`**:
  * `city`: Lọc theo thành phố (`HANOI`, `HCM`...)
  * `q`: Tìm kiếm tên rạp
  * `page`, `size`, `sort`
* **GET `/cinemas/{id}/showtimes`**:
  * `date` *(bắt buộc)*: Ngày chiếu (`YYYY-MM-DD`)
  * `movieId` *(tùy chọn)*: Lọc theo phim
  * `page`, `size`

---

### 🟢 MODULE 5: SUẤT CHIẾU & SƠ ĐỒ GHẾ (`/showtimes`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/showtimes/{id}` | ❌ Không | Chi tiết suất chiếu (phòng chiếu, giờ chiếu, định dạng) |
| `GET` | `/showtimes/{id}/seats` | ❌ Không | Sơ đồ ghế (Seat Map) & trạng thái từng ghế |

* **Trạng thái ghế**:
  * `AVAILABLE`: Ghế trống, có thể chọn
  * `SELECTED`: Ghế đang được giữ trong đơn đặt vé của người khác
  * `BOOKED`: Ghế đã thanh toán
  * `UNAVAILABLE`: Ghế bảo trì/không sử dụng

---

### 🟢 MODULE 6: BẮP NƯỚC F&B (`/products`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/products` | ❌ Không | Danh sách sản phẩm bắp rang, nước ngọt, combo |

#### Tham số Query:
* `category`: Thể loại (`POPCORN`, `DRINK`, `COMBO`)
* `status`: Trạng thái (`ACTIVE`, `INACTIVE`)
* `page`, `size`

---

### 🟢 MODULE 7: ĐẶT VÉ & ĐƠN HÀNG (`/bookings` - Yêu cầu Đăng nhập 🔒)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `POST` | `/bookings` | 🔒 Có | Tạo đơn đặt vé và giữ ghế tạm thời (Status `201 Created`) |
| `GET` | `/bookings/{id}` | 🔒 Có | Xem chi tiết đơn đặt vé |
| `PUT` | `/bookings/{id}/items` | 🔒 Có | Cập nhật số lượng bắp nước (F&B) trong đơn hàng |
| `PUT` | `/bookings/{id}/voucher` | 🔒 Có | Áp dụng mã giảm giá (voucher) vào đơn hàng |
| `POST` | `/bookings/{id}/payment` | 🔒 Có | Tạo giao dịch thanh toán cho đơn hàng (`WALLET` hoặc `VNPAY`) |
| `POST` | `/bookings/{id}/cancel` | 🔒 Có | Hủy đơn đặt vé khi còn trong thời gian giữ ghế |
| `POST` | `/bookings/{id}/refund` | 🔒 Có | Yêu cầu hoàn tiền vé đã thanh toán |
| `GET` | `/bookings/{id}/payment` | 🔒 Có | Xem thông tin các giao dịch thanh toán của đơn |
| `GET` | `/bookings/{id}/refund` | 🔒 Có | Xem trạng thái/tiến trình hoàn tiền của đơn |
| `GET` | `/bookings/{id}/tickets` | 🔒 Có | Xem danh sách vé điện tử đã xuất của đơn hàng |

#### Request Body mẫu:
* **POST `/bookings`** (Tạo đơn giữ ghế):
  ```json
  {
    "showTimeId": 1,
    "seatIds": [10, 11],
    "productItems": [
      {
        "productId": 1,
        "quantity": 2
      }
    ]
  }
  ```
* **PUT `/bookings/{id}/items`** (Cập nhật bắp nước):
  ```json
  {
    "items": [
      {
        "productId": 2,
        "quantity": 1
      }
    ]
  }
  ```
* **PUT `/bookings/{id}/voucher`** (Áp dụng voucher):
  ```json
  {
    "code": "GIAM50K"
  }
  ```
* **POST `/bookings/{id}/payment`** (Thanh toán đơn hàng):
  ```json
  {
    "method": "VNPAY"
  }
  ```
  *(Truyền `"method": "VNPAY"` để nhận link `paymentUrl` thanh toán VNPAY, hoặc `"method": "WALLET"` để trừ tiền trực tiếp trong ví)*.
* **POST `/bookings/{id}/cancel`** (Hủy đơn):
  ```json
  {
    "reason": "Khách muốn đổi sang suất chiếu khác"
  }
  ```
* **POST `/bookings/{id}/refund`** (Hoàn tiền):
  ```json
  {
    "reason": "Bận việc đột xuất không thể tham dự"
  }
  ```

---

### 🟢 MODULE 8: VÍ ĐIỆN TỬ (`/wallet` - Yêu cầu Đăng nhập 🔒)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/wallet` | 🔒 Có | Xem số dư ví và trạng thái ví của user |
| `POST` | `/wallet/top-up` | 🔒 Có | Tạo yêu cầu nạp tiền ví qua VNPAY (Status `201 Created`) |
| `GET` | `/wallet/top-up/{id}` | 🔒 Có | Kiểm tra trạng thái giao dịch nạp tiền theo ID giao dịch |
| `GET` | `/wallet/transactions` | 🔒 Có | Lịch sử biến động số dư và các giao dịch ví |

#### Request Body & Query:
* **POST `/wallet/top-up`**:
  ```json
  {
    "amount": 50000,
    "method": "VNPAY"
  }
  ```
  *(Response trả về `paymentUrl` của VNPAY Sandbox để mở thanh toán)*.
* **GET `/wallet/transactions`**:
  * `type`: Lọc loại giao dịch (`ADD_MONEY`, `PAY_BOOKING`, `REFUND`)
  * `from`: Từ ngày (`YYYY-MM-DD`)
  * `to`: Đến ngày (`YYYY-MM-DD`)
  * `page`, `size`

---

### 🟢 MODULE 9: THANH TOÁN & WEBHOOK VNPAY (`/payments`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/payments/{id}` | 🔒 Có | Xem chi tiết giao dịch thanh toán theo ID |
| `GET` | `/payments/webhook` | ❌ Không | VNPAY IPN Webhook (server-to-server callback) |
| `GET` | `/payments/vnpay-return` | ❌ Không | Callback trả về từ VNPAY dạng JSON API |

---

### 🟢 MODULE 10: VÉ XEM PHIM (`/tickets` - Yêu cầu Đăng nhập 🔒)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/tickets/{id}` | 🔒 Có | Chi tiết vé xem phim điện tử (mã vé, QR code, rạp, phòng, ghế) |

---

### 🟢 MODULE 11: TÀI KHOẢN NGƯỜI DÙNG (`/user` - Yêu cầu Đăng nhập 🔒)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/user` | 🔒 Có | Lấy thông tin hồ sơ tài khoản đang đăng nhập |
| `PATCH` | `/user` | 🔒 Có | Cập nhật thông tin cá nhân (họ tên, SĐT, ngày sinh) |
| `GET` | `/user/bookings` | 🔒 Có | Danh sách lịch sử đặt vé của user |
| `GET` | `/user/vouchers` | 🔒 Có | Danh sách ví voucher khuyến mãi của user |
| `POST` | `/user/change-password` | 🔒 Có | Đổi mật khẩu |

#### Request Body & Query:
* **PATCH `/user`**:
  ```json
  {
    "fullName": "Nguyễn Văn A",
    "phone": "0987654321",
    "dob": "2000-01-15"
  }
  ```
* **GET `/user/bookings`**:
  * `status`: Lọc theo trạng thái đơn (`PENDING`, `CONFIRMED`, `CANCELLED`, `REFUNDED`)
  * `from`, `to`, `page`, `size`, `sort`
* **GET `/user/vouchers`**:
  * `status`: Trạng thái voucher (`AVAILABLE`, `USED`, `EXPIRED`)
  * `page`, `size`
* **POST `/user/change-password`**:
  ```json
  {
    "oldPassword": "Password123@",
    "newPassword": "NewPassword456@"
  }
  ```

---

### 🟢 MODULE 12: GIAO DIỆN TRANG WEB (JSP)

| Method | Endpoint | Mô tả |
| :--- | :--- | :--- |
| `GET` | `/vn_return.jsp` | Trang hiển thị kết quả giao dịch thanh toán VNPAY (thành công/thất bại) |

---

## 💡 5. BẢNG MÃ LỖI THƯỜNG GẶP (HTTP STATUS CODES)

* `200 OK`: Thành công.
* `201 Created`: Tạo mới thành công (Đăng ký, Đặt vé, Nạp tiền, Review).
* `400 Bad Request`: Dữ liệu gửi lên không hợp lệ, thiếu tham số.
* `401 Unauthorized`: Chưa đăng nhập hoặc Token hết hạn / không hợp lệ.
* `403 Forbidden`: Không có quyền truy cập, hoặc tài khoản/ví bị khóa (`SUSPENDED`).
* `404 Not Found`: Không tìm thấy dữ liệu yêu cầu.
* `409 Conflict`: Xung đột dữ liệu (Email đã đăng ký, ghế đã bị đặt/giữ, đã review phim rồi).
* `410 Gone`: Hết thời gian giữ ghế của đơn hàng.
* `422 Unprocessable Entity`: Số dư ví không đủ, hoặc trạng thái không hợp lệ.
* `500 Internal Server Error`: Lỗi hệ thống server.
