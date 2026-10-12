# 🎬 CINEMA BACKEND - TÀI LIỆU TOÀN BỘ API ENDPOINTS

Tài liệu chi tiết toàn bộ các API Endpoints của hệ thống Cinema Backend.

---

## 🌐 1. THÔNG TIN CHUNG & CẤU HÌNH BASE URL

* **Môi trường Cloud (Render)**:  
  `https://be-cinema.onrender.com/api/v1`  
  *(Hoặc gọi không cần `/api/v1`: `https://be-cinema.onrender.com`)*
* **Môi trường Local (Tomcat / IntelliJ)**:  
  `http://localhost:8080/cinema_war_exploded/api/v1`

> [!NOTE]
> Hệ thống hỗ trợ cả 2 dạng đường dẫn:
> * Dạng chuẩn REST: `/api/v1/<resource>` (hoặc `/api/<resource>`)
> * Dạng gốc: `/<resource>`

---

## 🔐 2. CƠ CHẾ XÁC THỰC (AUTHENTICATION)

Hệ thống sử dụng **JWT (JSON Web Token)** để bảo mật:
* **Header**: `Authorization: Bearer <access_token>`
* **Cookie**: Tự động nhận diện cookie `access_token` nếu không truyền header.
* **Các route yêu cầu đăng nhập**: `/user/*`, `/wallet/*`, `/bookings/*`, `/tickets/*`, `/auth/change-password`, và đánh giá phim `POST /movies/{id}/reviews`.

---

## 📦 3. ĐỊNH DẠNG PHẢN HỒI CHUẨN (STANDARD RESPONSES)

### 3.1. Phản hồi thành công (ApiResponse)
```json
{
  "code": 200,
  "message": "OK",
  "data": { ... }
}
```

### 3.2. Phản hồi phân trang (PageResponse)
```json
{
  "code": 200,
  "message": "OK",
  "data": {
    "items": [ ... ],
    "meta": {
      "page": 0,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8
    }
  }
}
```

### 3.3. Phản hồi lỗi (ErrorResponse)
```json
{
  "code": 400,
  "message": "Thông báo lỗi chi tiết"
}
```

---

## 📋 4. DANH SÁCH CHI TIẾT TẤT CẢ API ENDPOINTS

---

### 🟢 MODULE 1: AUTHENTICATION (`/auth`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/v1/auth/register` | ❌ Không | Đăng ký tài khoản người dùng mới |
| `POST` | `/api/v1/auth/login` | ❌ Không | Đăng nhập tài khoản (trả về Access & Refresh Token) |
| `POST` | `/api/v1/auth/logout` | ❌ Không | Đăng xuất (xóa cookie và thu hồi token) |
| `POST` | `/api/v1/auth/refresh` | ❌ Không | Cấp mới Access Token bằng Refresh Token |
| `POST` | `/api/v1/auth/change-password` | 🔒 Có | Đổi mật khẩu người dùng đang đăng nhập |

#### Chi tiết Request Body:
* **POST `/api/v1/auth/register`**:
  ```json
  {
    "fullName": "Nguyễn Văn A",
    "email": "user@example.com",
    "password": "Password123@",
    "phone": "0987654321",
    "dob": "2000-01-15"
  }
  ```
* **POST `/api/v1/auth/login`**:
  ```json
  {
    "email": "user@example.com",
    "password": "Password123@"
  }
  ```
* **POST `/api/v1/auth/refresh`**:
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
  ```
* **POST `/api/v1/auth/change-password`**:
  ```json
  {
    "oldPassword": "OldPassword123@",
    "newPassword": "NewPassword456@"
  }
  ```

---

### 🟢 MODULE 2: TRANG CHỦ (`/home`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/home` | ❌ Không | Lấy dữ liệu trang chủ: Banners, phim đang chiếu, phim sắp chiếu, tin tức hot |

---

### 🟢 MODULE 3: PHIM & ĐÁNH GIÁ (`/movies`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/movies` | ❌ Không | Danh sách phim có tìm kiếm, lọc và phân trang |
| `GET` | `/api/v1/movies/{id}` | ❌ Không | Chi tiết thông tin một bộ phim theo ID |
| `GET` | `/api/v1/movies/{id}/showtimes` | ❌ Không | Danh sách suất chiếu của phim theo ngày |
| `GET` | `/api/v1/movies/{id}/reviews` | ❌ Không | Danh sách đánh giá & bình luận của bộ phim |
| `POST` | `/api/v1/movies/{id}/reviews` | 🔒 Có | Gửi đánh giá / bình luận mới cho bộ phim |

#### Tham số Query:
* **GET `/api/v1/movies`**:
  * `q`: Từ khóa tìm kiếm tên phim
  * `genre`: Mã thể loại (ví dụ: `ACTION`, `HORROR`)
  * `status`: Trạng thái (`NOW_SHOWING`, `COMING_SOON`)
  * `page`: Trang (bắt đầu từ 0)
  * `size`: Số bản ghi mỗi trang (mặc định 20)
  * `sort`: Sắp xếp (`releaseDate,desc`, `title,asc`, ...)
* **GET `/api/v1/movies/{id}/showtimes`**:
  * `date` *(bắt buộc)*: Ngày chiếu (`YYYY-MM-DD`, ví dụ: `2026-10-15`)
  * `cinemaId` *(tùy chọn)*: Lọc theo rạp
  * `page`, `size`
* **POST `/api/v1/movies/{id}/reviews`** (Body):
  ```json
  {
    "rating": 5,
    "comment": "Phim cực kỳ xuất sắc, âm thanh sống động!"
  }
  ```

---

### 🟢 MODULE 4: RẠP CHIẾU (`/cinemas`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/cinemas` | ❌ Không | Danh sách rạp chiếu (lọc theo tỉnh/thành phố) |
| `GET` | `/api/v1/cinemas/{id}` | ❌ Không | Chi tiết thông tin rạp chiếu theo ID |
| `GET` | `/api/v1/cinemas/{id}/showtimes` | ❌ Không | Lịch chiếu các phim tại rạp theo ngày |

#### Tham số Query:
* **GET `/api/v1/cinemas`**:
  * `city`: Lọc theo tỉnh thành (ví dụ: `HANOI`, `HCM`)
  * `q`: Tìm kiếm tên rạp
  * `page`, `size`, `sort`
* **GET `/api/v1/cinemas/{id}/showtimes`**:
  * `date` *(bắt buộc)*: Ngày chiếu (`YYYY-MM-DD`)
  * `movieId` *(tùy chọn)*: Lọc theo phim cụ thể
  * `page`, `size`

---

### 🟢 MODULE 5: SUẤT CHIẾU & SƠ ĐỒ GHẾ (`/showtimes`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/showtimes/{id}` | ❌ Không | Chi tiết suất chiếu (phòng chiếu, giờ chiếu, giá vé) |
| `GET` | `/api/v1/showtimes/{id}/seats` | ❌ Không | Sơ đồ ghế (Seat Map) và trạng thái giữ ghế/đã đặt |

* **Trạng thái ghế trả về**:
  * `AVAILABLE`: Ghế còn trống, có thể chọn
  * `SELECTED`: Ghế đang được tạm giữ trong đơn đặt của khách khác
  * `BOOKED`: Ghế đã thanh toán xong
  * `UNAVAILABLE`: Ghế bị vô hiệu hóa/bảo trì

---

### 🟢 MODULE 6: BẮP NƯỚC & COMBO F&B (`/products`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/products` | ❌ Không | Danh sách bắp rang, nước ngọt, combo khuyến mãi |

#### Tham số Query:
* `category`: Phân loại (ví dụ: `POPCORN`, `DRINK`, `COMBO`)
* `status`: Trạng thái sản phẩm (`ACTIVE`, `INACTIVE`)
* `page`, `size`

---

### 🟢 MODULE 7: ĐẶT VÉ & ĐƠN HÀNG (`/bookings` - Yêu cầu Đăng nhập 🔒)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `POST` | `/api/v1/bookings` | 🔒 Có | Khởi tạo đơn đặt vé & giữ ghế tạm thời (10-15 phút) |
| `GET` | `/api/v1/bookings/{id}` | 🔒 Có | Chi tiết đơn đặt vé |
| `PUT` | `/api/v1/bookings/{id}/items` | 🔒 Có | Cập nhật số lượng bắp nước (F&B) trong đơn |
| `PUT` | `/api/v1/bookings/{id}/voucher` | 🔒 Có | Áp dụng mã giảm giá (Voucher) vào đơn hàng |
| `POST` | `/api/v1/bookings/{id}/payment` | 🔒 Có | Tạo thanh toán cho đơn hàng (`WALLET` hoặc `VNPAY`) |
| `POST` | `/api/v1/bookings/{id}/cancel` | 🔒 Có | Hủy đơn đặt vé khi còn trong thời gian giữ ghế |
| `POST` | `/api/v1/bookings/{id}/refund` | 🔒 Có | Yêu cầu hoàn tiền vé đã thanh toán |
| `GET` | `/api/v1/bookings/{id}/payment` | 🔒 Có | Lịch sử / Chi tiết giao dịch thanh toán của đơn |
| `GET` | `/api/v1/bookings/{id}/refund` | 🔒 Có | Xem tiến trình hoàn tiền của đơn hàng |
| `GET` | `/api/v1/bookings/{id}/tickets` | 🔒 Có | Danh sách vé điện tử đã xuất thuộc về đơn này |

#### Chi tiết Request Body:
* **POST `/api/v1/bookings`** (Tạo đơn giữ ghế):
  ```json
  {
    "showTimeId": 1,
    "seatIds": [12, 13],
    "productItems": [
      {
        "productId": 1,
        "quantity": 2
      }
    ]
  }
  ```
* **PUT `/api/v1/bookings/{id}/items`** (Cập nhật bắp nước):
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
* **PUT `/api/v1/bookings/{id}/voucher`** (Áp dụng voucher):
  ```json
  {
    "code": "CINEMA50K"
  }
  ```
* **POST `/api/v1/bookings/{id}/payment`** (Thanh toán đơn):
  ```json
  {
    "method": "VNPAY"
  }
  ```
  *(Hỗ trợ `"method": "VNPAY"` trả về `paymentUrl`, hoặc `"method": "WALLET"` trừ tiền trực tiếp từ ví)*.
* **POST `/api/v1/bookings/{id}/cancel`** (Hủy đơn):
  ```json
  {
    "reason": "Khách đổi kế hoạch"
  }
  ```
* **POST `/api/v1/bookings/{id}/refund`** (Hoàn tiền):
  ```json
  {
    "reason": "Bận việc đột xuất không thể đi xem"
  }
  ```

---

### 🟢 MODULE 8: VÍ ĐIỆN TỬ (`/wallet` - Yêu cầu Đăng nhập 🔒)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/wallet` | 🔒 Có | Xem số dư ví và trạng thái ví của người dùng |
| `POST` | `/api/v1/wallet/top-up` | 🔒 Có | Tạo giao dịch nạp tiền vào ví qua VNPAY |
| `GET` | `/api/v1/wallet/top-up/{id}` | 🔒 Có | Kiểm tra trạng thái giao dịch nạp tiền theo ID |
| `GET` | `/api/v1/wallet/transactions` | 🔒 Có | Lịch sử biến động số dư và các giao dịch ví |

#### Chi tiết Request Body & Params:
* **POST `/api/v1/wallet/top-up`**:
  ```json
  {
    "amount": 100000,
    "method": "VNPAY"
  }
  ```
  *(Response sẽ trả về `paymentUrl` để mở VNPAY sandbox)*.
* **GET `/api/v1/wallet/transactions`**:
  * `type`: Lọc theo loại (`ADD_MONEY`, `PAY_BOOKING`, `REFUND`)
  * `from`: Từ ngày (`YYYY-MM-DD`)
  * `to`: Đến ngày (`YYYY-MM-DD`)
  * `page`, `size`

---

### 🟢 MODULE 9: THANH TOÁN & WEBHOOK VNPAY (`/payments`)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/payments/{id}` | 🔒 Có | Xem chi tiết giao dịch thanh toán |
| `GET` | `/api/v1/payments/webhook` | ❌ Không | VNPAY IPN Webhook (server-to-server callback) |
| `GET` | `/api/v1/payments/vnpay-return` | ❌ Không | Callback trả về từ VNPAY dạng JSON API |

---

### 🟢 MODULE 10: VÉ XEM PHIM (`/tickets` - Yêu cầu Đăng nhập 🔒)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/tickets/{id}` | 🔒 Có | Xem chi tiết vé điện tử (mã vé, QR code, phim, rạp, ghế) |

---

### 🟢 MODULE 11: THÔNG TIN TÀI KHOẢN NGƯỜI DÙNG (`/user` - Yêu cầu Đăng nhập 🔒)

| Method | Endpoint | Yêu cầu Auth | Mô tả |
| :--- | :--- | :---: | :--- |
| `GET` | `/api/v1/user` | 🔒 Có | Xem thông tin hồ sơ người dùng đang đăng nhập |
| `PATCH` | `/api/v1/user` | 🔒 Có | Cập nhật thông tin cá nhân (họ tên, SĐT, ngày sinh) |
| `GET` | `/api/v1/user/bookings` | 🔒 Có | Lịch sử tất cả các đơn đặt vé của người dùng |
| `GET` | `/api/v1/user/vouchers` | 🔒 Có | Danh sách ví voucher / khuyến mãi của người dùng |
| `POST` | `/api/v1/user/change-password` | 🔒 Có | Đổi mật khẩu |

#### Chi tiết Request Body & Params:
* **PATCH `/api/v1/user`**:
  ```json
  {
    "fullName": "Nguyễn Văn B",
    "phone": "0912345678",
    "dob": "1999-05-20"
  }
  ```
* **GET `/api/v1/user/bookings`**:
  * `status`: Lọc theo trạng thái đơn (`PENDING`, `CONFIRMED`, `CANCELLED`, `REFUNDED`)
  * `from`, `to`, `page`, `size`, `sort`
* **GET `/api/v1/user/vouchers`**:
  * `status`: Lọc trạng thái voucher (`AVAILABLE`, `USED`, `EXPIRED`)
  * `page`, `size`

---

### 🟢 MODULE 12: GIAO DIỆN WEB TRẢ VỀ (JSP)

| Method | URL | Mô tả |
| :--- | :--- | :--- |
| `GET` | `/vn_return.jsp` | Trang hiển thị kết quả giao dịch thanh toán VNPAY (thành công/thất bại) |

---

## 💡 5. MÃ LỖI THƯỜNG GẶP (HTTP STATUS CODES)

* `200 OK`: Xử lý thành công.
* `201 Created`: Tạo mới thành công (Đăng ký, Đặt vé, Nạp tiền, Review).
* `400 Bad Request`: Thiếu tham số, định dạng ngày giờ không hợp lệ, dữ liệu JSON sai.
* `401 Unauthorized`: Chưa đăng nhập hoặc Access Token hết hạn / không hợp lệ.
* `403 Forbidden`: Không có quyền truy cập tài nguyên của người khác, hoặc ví/tài khoản bị khóa.
* `404 Not Found`: Không tìm thấy tài nguyên (phim, rạp, đơn hàng, người dùng...).
* `409 Conflict`: Xung đột dữ liệu (Email đã tồn tại, ghế đã bị người khác chọn/đặt, đã review phim này rồi).
* `410 Gone`: Đơn hàng đã quá hạn giữ ghế (hết thời gian thanh toán).
* `422 Unprocessable Entity`: Số dư ví không đủ, hoặc dữ liệu không hợp lệ.
* `500 Internal Server Error`: Lỗi hệ thống server.
