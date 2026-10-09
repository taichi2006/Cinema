# 🎬 Cinema API 

Một ứng dụng Backend Web phục vụ hệ thống quản lý rạp chiếu phim. Được xây dựng dựa trên kiến trúc nhóm theo tính năng (Feature-based/Domain-driven).

## 🚀 Công nghệ sử dụng
- **Ngôn ngữ:** Java 21
- **Core Framework:** Jakarta Servlet API (thuần)
- **Cơ sở dữ liệu:** PostgreSQL
- **ORM & Connection Pool:** Hibernate 6.5 & HikariCP
- **JSON Parser:** Jackson Databind
- **Quản lý môi trường:** Dotenv (Tệp `.env`)

## 📂 Cấu trúc dự án
Dự án được tổ chức theo module tính năng, giúp dễ dàng mở rộng và bảo trì:
```text
src/main/java/com/cinema/
├── auth/          # Xử lý xác thực người dùng, JWT token
├── booking/       # Xử lý quy trình đặt vé, chọn ghế
├── movie/         # Quản lý danh sách phim (Movie, DTO, DAO, Service, Controller)
├── payment/       # Xử lý thanh toán
├── showtime/      # Quản lý lịch chiếu phim
└── config/        # Chứa các cấu hình toàn cục (DB, ApiPrefixFilter)
```

## 🔌 API Endpoints
Toàn bộ API được tự động cấu hình tiền tố `/api/` qua Filter.
Ví dụ:
- `GET /api/movie` - Lấy danh sách phim

*(Để thêm API mới, chỉ cần khai báo `@WebServlet("/endpoint")` trong Controller, hệ thống tự động nhận diện thành `/api/endpoint`)*

## 🛠️ Hướng dẫn cài đặt

1. **Cấu hình biến môi trường:**
Tạo file `.env` ở thư mục gốc của dự án (ngang hàng với `pom.xml`) với nội dung:
```env
DB_URL=jdbc:postgresql://<host>:<port>/<dbname>
DB_USERNAME=your_username
DB_PASSWORD=your_password
```

2. **Khởi chạy ứng dụng:**
Đây là một dự án Maven (đóng gói dạng `war`).
- Tải lại các dependency: `mvn clean install`
- Deploy tệp WAR lên web server như **Apache Tomcat** hoặc chạy trực tiếp bằng IDE (IntelliJ IDEA) thông qua cấu hình Tomcat Server.
