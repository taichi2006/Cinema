# 📧 Module Gửi Email Vé Xem Phim (`com.cinema.ticket.Email`)

Thư mục này chứa toàn bộ mã nguồn xử lý gửi email xác nhận vé xem phim điện tử cho khách hàng sau khi thanh toán thành công, sử dụng **JavaMail SMTP** kết nối với máy chủ **Gmail** và **Mật khẩu ứng dụng của Google (Google App Password)**.

---

## 📁 Cấu Trúc Thành Phần

```text
com.cinema.ticket.Email/
├── MailConfig.java             # Nạp cấu hình SMTP từ file .env / System Environment
├── MailUtilGmail.java          # Tiện ích gửi mail qua Google SMTP (đồng bộ & bất đồng bộ)
├── TicketEmailTemplate.java    # Mẫu email HTML thiết kế giao diện Cinema sang trọng
├── TicketEmailService.java     # Service nghiệp vụ tổng hợp dữ liệu & gửi email vé
└── README.md                   # Hướng dẫn sử dụng & cấu hình
```

---

## ⚙️ Hướng Dẫn Cấu Hình Mật Khẩu Ứng Dụng Google (Google App Password)

Google không cho phép đăng nhập SMTP bằng mật khẩu tài khoản trực tiếp, bạn cần tạo **Mật khẩu ứng dụng 16 ký tự**:

1. Đăng nhập vào tài khoản Google bạn muốn dùng làm email gửi: [https://myaccount.google.com/](https://myaccount.google.com/)
2. Vào mục **Bảo mật (Security)** $\rightarrow$ Bật **Xác minh 2 bước (2-Step Verification)** (nếu chưa bật).
3. Tìm kiếm **Mật khẩu ứng dụng (App passwords)** hoặc truy cập: [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords).
4. Đặt tên ứng dụng (ví dụ: `Cinema Booking`) $\rightarrow$ Bấm **Tạo (Create)**.
5. Google sẽ cấp cho bạn một chuỗi mật khẩu 16 ký tự (ví dụ: `abcd efgh ijkl mnop`).
6. Thêm các biến sau vào file `.env` ở thư mục gốc của dự án:

```env
# JavaMail SMTP (Gmail với Google App Password):
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=email_cua_ban@gmail.com
MAIL_PASSWORD=abcd efgh ijkl mnop
MAIL_FROM_NAME=Cinema Star Booking
```

---

## 🚀 Cách Sử Dụng Trong Dự Án

### Cách 1: Gửi tự động theo `bookingId` và `userId` (Khuyên dùng)
Sau khi đơn hàng chuyển sang trạng thái `CONFIRMED` / `PAID`:

```java
import com.cinema.ticket.Email.TicketEmailService;

TicketEmailService emailService = new TicketEmailService();

// Gửi bất đồng bộ (chạy ngầm, không làm nghẽn request thanh toán)
emailService.sendTicketEmailByBookingAsync(bookingId, userId);
```

### Cách 2: Tự truyền thông tin tùy chỉnh
```java
import com.cinema.ticket.Email.TicketEmailService;

TicketEmailService emailService = new TicketEmailService();

emailService.sendTicketEmailAsync(
    "khachhang@gmail.com",
    "Nguyễn Văn A",
    "Mai",
    "Galaxy Nguyễn Du",
    "Cinema 1",
    LocalDateTime.now().plusDays(1),
    ticketsList,
    new BigDecimal("190000")
);
```

### Cách 3: Gửi email HTML bất kỳ qua `MailUtilGmail`
```java
import com.cinema.ticket.Email.MailUtilGmail;

// Gửi bất đồng bộ
MailUtilGmail.sendHtmlMailAsync(
    "khachhang@gmail.com",
    "Tiêu đề email",
    "<h1>Nội dung HTML</h1>"
);
```
