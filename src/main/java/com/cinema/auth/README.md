# Tài Liệu Kỹ Thuật: Module Auth

Tài liệu mô tả chi tiết kiến trúc và chuẩn Request/Response của hệ thống Xác thực (Auth), đồng bộ 100% với đặc tả API của team (file Excel) và cơ sở dữ liệu PostgreSQL (schema `cinema`).

> **Lưu ý kiến trúc quan trọng**:
> - Hệ thống **KHÔNG CÓ ROLE** (bảng `cinema.roles` và trường `role_id` đã được loại bỏ hoàn toàn).
> - Access Token chỉ chứa subject là `userId` (không chứa claim role).
> - Thao tác trực tiếp với các bảng: `cinema.users`, `cinema.wallets` và `cinema.refresh_tokens`.

---

## 1. Kiến trúc chung
- **Route Prefix**: Hỗ trợ đồng thời `/api/v1/auth/*`, `/api/auth/*` và `/auth/*`.
- **Controller (`AuthController`)**: Tiếp nhận HTTP Request, trích xuất IP / User-Agent, parse DTO, gọi Service, thiết lập Cookie và trả về JSON chuẩn.
- **Service (`AuthService`)**: Xử lý logic đăng ký, đăng nhập, cấp phát/xoay vòng (rotate) refresh token, đăng xuất và đổi mật khẩu.
- **DAO (`AuthDAO`)**: Thao tác dữ liệu với bảng `cinema.users`, `cinema.wallets` và `cinema.refresh_tokens`.
- **JWT Helper (`JwtUtil`)**: Ký tạo và giải mã Access Token (15 phút), Refresh Token (7 ngày), tính hash SHA-256 cho refresh token và quản lý HTTP Cookie (HttpOnly).
- **Filter (`AuthFilter` & `ApiPrefixFilter`)**: Kiểm tra JWT Access Token ở header `Authorization: Bearer <token>` hoặc Cookie `access_token`, trích xuất `userId` vào `request.setAttribute("userId", userId)`.

---

## 2. Đặc tả API chuẩn theo team

### 2.1. Đăng ký tài khoản (`POST /api/v1/auth/register`)
- **Mô tả**: Đăng ký người dùng mới, tự động khởi tạo ví (balance = 0.00, status = ACTIVE).
- **Auth (Middleware)**: Không
- **Status Codes**: `201`, `400`, `409`
- **Request Body (JSON)**:
  ```json
  {
    "fullName": "Nguyễn Văn A",
    "email": "a@gmail.com",
    "phone": "0901234567",
    "password": "P@ssw0rd123",
    "dob": "2000-01-15"
  }
  ```
- **Response Success (HTTP 201)**:
  ```json
  {
    "success": true,
    "data": {
      "userId": 1,
      "fullName": "Nguyễn Văn A",
      "email": "a@gmail.com",
      "status": "ACTIVE"
    }
  }
  ```
- **Response Error (HTTP 409)**:
  ```json
  {
    "success": false,
    "status": 409,
    "error": "Email đã được đăng ký"
  }
  ```

---

### 2.2. Đăng nhập (`POST /api/v1/auth/login`)
- **Mô tả**: Xác thực tài khoản, trả về token và lưu cookie `access_token`, `refresh_token`.
- **Auth (Middleware)**: Không
- **Status Codes**: `200`, `401`, `403`
- **Request Body (JSON)**:
  ```json
  {
    "email": "a@gmail.com",
    "password": "P@ssw0rd123"
  }
  ```
- **Response Success (HTTP 200)**:
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "eyJ...",
      "refreshToken": "eyJ...",
      "tokenType": "Bearer",
      "expiresIn": 900,
      "user": {
        "userId": 1,
        "email": "a@gmail.com"
      }
    }
  }
  ```
- **Response Error (HTTP 401)**:
  ```json
  {
    "success": false,
    "status": 401,
    "error": "Email hoặc mật khẩu không đúng"
  }
  ```
- **Response Error (HTTP 403)**:
  ```json
  {
    "success": false,
    "status": 403,
    "error": "Tài khoản đã bị khóa"
  }
  ```

---

### 2.3. Làm mới access token (`POST /api/v1/auth/refresh`)
- **Mô tả**: Cấp access token mới từ refresh token (hỗ trợ đọc từ body hoặc cookie).
- **Auth (Middleware)**: Không
- **Status Codes**: `200`, `401`
- **Request Body (JSON)**:
  ```json
  {
    "refreshToken": "eyJ..."
  }
  ```
- **Response Success (HTTP 200)**:
  ```json
  {
    "success": true,
    "data": {
      "accessToken": "eyJ...",
      "refreshToken": "eyJ...",
      "expiresIn": 900
    }
  }
  ```
- **Response Error (HTTP 401)**:
  ```json
  {
    "success": false,
    "status": 401,
    "error": "Refresh token đã hết hạn"
  }
  ```

---

### 2.4. Đăng xuất (`POST /api/v1/auth/logout`)
- **Mô tả**: Thu hồi refresh token trong DB và xóa cookie trên client.
- **Auth (Middleware)**: Có (Bearer JWT hoặc Cookie `access_token`)
- **Status Codes**: `200`, `401`
- **Request Body (JSON)**: Không có
- **Response Success (HTTP 200)**:
  ```json
  {
    "success": true,
    "data": null
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

### 2.5. Đổi mật khẩu (`POST /api/user/change-password` hoặc `POST /user/change-password`)
- **Mô tả**: Đổi mật khẩu tài khoản và thu hồi toàn bộ refresh token hiện có.
- **Auth (Middleware)**: Có (Bearer JWT)
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
