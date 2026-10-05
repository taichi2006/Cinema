# Tài Liệu Kỹ Thuật: Module Auth & User

Tài liệu này mô tả chi tiết kiến trúc và luồng hoạt động của hệ thống Xác thực (Auth) và Người dùng (User). Phiên bản này đã được **tối giản hóa tối đa** để đảm bảo dễ hiểu, dễ maintain cho team, sử dụng **JPA (Hibernate)** thay vì JDBC thuần.

---

## 1. Kiến trúc chung
Hệ thống sử dụng mô hình MVC với **Thin Controller** và **Thick Service**:
- **Controller**: Chỉ làm nhiệm vụ tiếp nhận HTTP Request, parse JSON sang DTO, gọi Service, và trả về `ApiResponse`. Không chứa logic nghiệp vụ.
- **Service**: Xử lý toàn bộ logic nghiệp vụ, gọi Database thông qua `JPAUtil.getEntityManager()`.
- **Entity**: Đại diện cho cấu trúc bảng trong Database (Chỉ map các trường thật sự cần thiết).
- **Filter**: Chặn ở cửa ngõ các API cần bảo mật để kiểm tra quyền truy cập.

---

## 2. Module `user` (Quản lý dữ liệu người dùng)

Để giảm thiểu sự phức tạp, các Entity chỉ ánh xạ (map) đúng những cột thật sự cần thiết phục vụ cho việc Đăng nhập và Quản lý Profile.

- **`User.java`**: Map với bảng `cinema.users`. Chỉ chứa các trường: `id`, `role`, `email`, `passwordHash`, `fullName`, `status`. Tuyệt đối không nhồi nhét các logic thừa thãi.
- **`Role.java`**: Map với bảng `cinema.roles` (quan hệ N-1 với User).
- **`UserService.java`**: Xử lý các nghiệp vụ của User hiện tại (Lấy thông tin cá nhân `getMe`, Đổi tên `updateProfile`). Sử dụng toàn bộ lệnh JPA thuần túy (`em.find()`, `user.set...`).

---

## 3. Module `auth` (Hệ thống đăng nhập bằng Token)

Chúng ta sử dụng cơ chế **JWT Bearer Authorization** với Access Token (ngắn hạn) và Refresh Token (dài hạn).

### 3.1. Các Endpoint cung cấp
1. **`POST /api/auth/register`**: Tạo tài khoản với mật khẩu được mã hóa tự động bằng `BCrypt`.
2. **`POST /api/auth/login`**: Kiểm tra Email/Password và `status` (bị khóa hay không). Nếu thành công, Server trả về thông tin User kèm `accessToken` và `refreshToken`.
3. **`POST /api/auth/refresh`**: Cấp lại Access Token và Refresh Token mới dựa vào Refresh Token cũ.
4. **`POST /api/auth/logout`**: Đăng xuất thành công. Client tự xóa token.

### 3.2. Tiện ích `JwtUtil.java`
- Chỉ đảm nhận 2 việc: Ký tạo (Generate) token và Đọc (Parse) token.
- Token chứa 2 thông tin cơ bản: `userId` và `role`.

### 3.3. Tổ chức DTO (Data Transfer Object)
- Tất cả các Object gửi lên Server và trả về đều được chuẩn hóa nằm trong:
  - `com.cinema.auth.dto.request`: `LoginRequest`, `RegisterRequest`, `RefreshRequest`.
  - `com.cinema.auth.dto.response`: `LoginResponse`, `AuthUserResponse`.

---

## 4. Bảo mật với `AuthFilter.java` (Cực kỳ quan trọng)

Thay vì mỗi API phải tự kiểm tra xem User đã đăng nhập chưa, chúng ta sử dụng **Filter**.

- `AuthFilter` được đăng ký để tự động chặn tất cả các Request đi vào `/user/*`, `/booking/*`, `/admin/*`.
- Khi có Request đi qua, nó sẽ tự động đọc header `Authorization` (định dạng `Bearer <token>`), giải mã token.
- **Nếu Token hợp lệ**: Nó nhét `userId` và `role` vào `request.setAttribute` rồi mở cửa cho đi tiếp vào Controller.
- **Nếu Token sai / hết hạn / không có**: Nó ném lỗi `401 Unauthorized` và đuổi về ngay lập tức. Cửa đóng.

**💡 Hệ quả cực hay dành cho Developer:**
Bất cứ khi nào bạn viết một API mới (ví dụ Lấy danh sách vé đã đặt), bạn không cần quan tâm đến JWT hay bảo mật nữa. Bạn chỉ cần gõ đúng 1 dòng:
```java
long userId = (long) req.getAttribute("userId");
```
Là bạn đã có trong tay ID của người đang gọi API!

---

## 5. Xử lý lỗi & Trả về (Module `common`)

- Mọi API thành công phải được bọc trong `ApiResponse.ok(data)` hoặc `ApiResponse.success("Thông báo")`.
- Nếu có lỗi nghiệp vụ (Vd: Dữ liệu sai), không dùng `if-else` trả về HTTP status. Hãy quăng lỗi trực tiếp: 
  ```java
  throw ApiException.badRequest("Mật khẩu không đúng");
  ```
- **`ErrorHandler.java`** sẽ tự động "hứng" toàn bộ các lỗi bị quăng ra này, và biến nó thành chuỗi JSON chuẩn có dạng:
  ```json
  { "success": false, "status": 400, "error": "Mật khẩu không đúng" }
  ```

### 5.1. Quản lý lỗi Xác thực (`AuthException.java`)
Để code được gọn gàng và tránh hardcode các câu thông báo lỗi xác thực ở nhiều nơi, toàn bộ lỗi liên quan đến Auth đã được gom chung vào class `AuthException` (kế thừa từ `ApiException`):

Thay vì viết dài dòng:
```java
throw ApiException.unauthorized("Tài khoản đã bị khóa");
```
Bây giờ chỉ cần gọi:
```java
throw AuthException.accountLocked();
```
Các hàm đã được định nghĩa sẵn bao gồm:
- `unauthorized()`: Chưa đăng nhập.
- `invalidCredentials()`: Sai email hoặc mật khẩu.
- `accountLocked()`: Tài khoản bị khóa.
- `invalidToken()`: Token sai hoặc hết hạn.

---

## Lời khuyên cho Frontend Team
- **Lưu Access Token**: Sau khi có được `accessToken` từ API Login, Frontend nên lưu token này (vd: `localStorage` hoặc `sessionStorage`).
- **Gửi Token lên Server**: Trong tất cả các API cần xác thực quyền, bắt buộc phải đính kèm header:
  `Authorization: Bearer <accessToken>`
- Khi gọi Logout, Frontend chỉ cần tự xóa Token dưới client mà không cần bắt buộc phụ thuộc hoàn toàn vào Response.
