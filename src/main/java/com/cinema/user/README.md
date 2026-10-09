# Tài Liệu Kỹ Thuật: Module User

Tài liệu mô tả chi tiết chuẩn Request/Response của module **User** theo bảng đặc tả API (Excel) và cơ sở dữ liệu PostgreSQL (schema `cinema`).

> **Lưu ý kiến trúc quan trọng**:
> - Hệ thống **KHÔNG CÓ ROLE** (bảng `cinema.roles` và trường `role_id` đã được loại bỏ hoàn toàn).
> - Mọi người dùng đã xác thực (có Bearer JWT hợp lệ) đều có quyền truy cập các endpoint thuộc `/user`.
> - **Cơ chế định tuyến API**: Đã có `ApiPrefixFilter` xử lý tiền tố `/api/*` toàn cục và forward tới Servlet tương ứng (`/user`, `/user/*`).
> - **Servlet Mapping**: `@WebServlet(urlPatterns = {"/user", "/user/*"})`. Không cần thêm tiền tố `api` trong servlet (tránh lỗi lặp đường dẫn `api/api/user`) và hệ thống không sử dụng phiên bản `v1`.
> - Dữ liệu bảng `cinema.users`: `(user_id, full_name, email, phone, password_hash, dob, avatar_url, status, created_at)`.

---

## 1. Danh sách Endpoints `/user`

### 1.1. Xem hồ sơ cá nhân (`GET /api/user` hoặc `GET /user`)
- **Mô tả**: Xem thông tin tài khoản hiện tại.
- **Auth (Middleware)**: Có (Bearer JWT hoặc Cookie `access_token`).
- **Status Codes**: `200`, `401`
- **Request Body**: Không có
- **Response Success (HTTP 200)**:
  ```json
  {
    "success": true,
    "data": {
      "userId": 1,
      "fullName": "Nguyễn Văn A",
      "email": "a@gmail.com",
      "phone": "0901234567",
      "status": "ACTIVE"
    }
  }
  ```
- **Response Error (HTTP 401)**:
  ```json
  {
    "success": false,
    "status": 401,
    "error": "Chưa đăng nhập"
  }
  ```

---

### 1.2. Cập nhật hồ sơ (`PATCH /api/user` hoặc `PATCH /user`)
- **Mô tả**: Cập nhật thông tin họ tên, số điện thoại hoặc ngày sinh.
- **Auth (Middleware)**: Có (Bearer JWT hoặc Cookie `access_token`).
- **Status Codes**: `200`, `400`, `401`
- **Request Body (JSON)**:
  ```json
  {
    "fullName": "Nguyễn Văn B",
    "phone": "0987654321",
    "dob": "1999-05-20"
  }
  ```
- **Response Success (HTTP 200)**:
  ```json
  {
    "success": true,
    "data": {
      "userId": 1,
      "fullName": "Nguyễn Văn B"
    }
  }
  ```
- **Response Error (HTTP 400)**:
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Họ tên phải từ 2-100 ký tự"
  }
  ```

---

### 1.3. Đổi mật khẩu (`POST /api/user/change-password` hoặc `POST /user/change-password`)
- **Mô tả**: Đổi mật khẩu người dùng đang đăng nhập và thu hồi refresh token.
- **Auth (Middleware)**: Có (Bearer JWT hoặc Cookie `access_token`).
- **Status Codes**: `200`, `400`, `401`, `422`
- **Request Body (JSON)**:
  ```json
  {
    "currentPassword": "P@ssw0rd123",
    "newPassword": "NewP@ssw0rd456",
    "confirmPassword": "NewP@ssw0rd456"
  }
  ```
- **Response Success (HTTP 200)**:
  ```json
  {
    "success": true,
    "data": null
  }
  ```
- **Response Error (HTTP 422)**:
  ```json
  {
    "success": false,
    "status": 422,
    "error": "Mật khẩu hiện tại không đúng"
  }
  ```

---

### 1.4. Lịch sử đặt vé (`GET /api/user/bookings` hoặc `GET /user/bookings`)
- **Mô tả**: Lấy danh sách booking của người dùng theo phân trang và bộ lọc.
- **Auth (Middleware)**: Có (Bearer JWT hoặc Cookie `access_token`).
- **Query Params**:
  * `page`: Trang hiện tại (mặc định: `0`)
  * `size`: Số phần tử trên 1 trang (mặc định: `20`)
  * `status`: `PENDING`, `CONFIRMED`, `CANCELLED`
- **Status Codes**: `200`, `401`
- **Response Success (HTTP 200)**:
  ```json
  {
    "success": true,
    "data": {
      "items": [
        {
          "bookingId": 1001,
          "status": "CONFIRMED",
          "totalAmount": 320000
        }
      ],
      "meta": {
        "page": 0,
        "size": 20,
        "totalElements": 5
      }
    }
  }
  ```
- **Response Error (HTTP 401)**:
  ```json
  {
    "success": false,
    "status": 401,
    "error": "Chưa đăng nhập"
  }
  ```

---

### 1.5. Voucher cá nhân (`GET /api/user/vouchers` hoặc `GET /user/vouchers`)
- **Mô tả**: Lấy danh sách voucher người dùng đang sở hữu.
- **Auth (Middleware)**: Có (Bearer JWT hoặc Cookie `access_token`).
- **Query Params**:
  * `page`: Trang hiện tại (mặc định: `0`)
  * `size`: Số phần tử trên 1 trang (mặc định: `20`)
  * `status`: `UNUSED`, `USED`, `EXPIRED`
- **Status Codes**: `200`, `401`
- **Response Success (HTTP 200)**:
  ```json
  {
    "success": true,
    "data": {
      "items": [
        {
          "voucherId": 1,
          "code": "SUMMER2024",
          "status": "UNUSED"
        }
      ]
    }
  }
  ```
- **Response Error (HTTP 401)**:
  ```json
  {
    "success": false,
    "status": 401,
    "error": "Chưa đăng nhập"
  }
  ```
