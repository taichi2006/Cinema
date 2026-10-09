# 📋 Kế hoạch triển khai & Cập nhật Module `wallet`

> **Tài liệu tham chiếu chuẩn duy nhất của Cơ sở Dữ liệu**:
> - [cinema sql.txt](file:///d:/IT1_UTE/HK5_26_27/lap_trinh_web/project/cinema_system/cinema%20sql.txt) (File thể hiện chuẩn CSDL PostgreSQL hiện tại)
> - [swagger_cinema.yaml](file:///d:/IT1_UTE/HK5_26_27/lap_trinh_web/project/cinema_system/swagger_cinema.yaml) (Đặc tả API)
> - [UMLClassDiagram.drawio.png](file:///d:/IT1_UTE/HK5_26_27/lap_trinh_web/project/cinema_system/UMLClassDiagram.drawio.png) (Mô hình lớp)

---

## 1. Chi tiết Thay đổi CSDL so với thiết kế cũ

Đối chiếu trực tiếp với [cinema sql.txt](file:///d:/IT1_UTE/HK5_26_27/lap_trinh_web/project/cinema_system/cinema%20sql.txt):

### 1.1 Bảng `cinema.wallets`
* **Định nghĩa chuẩn trong `cinema sql.txt`**:
  ```sql
  CREATE TABLE wallets (
      wallet_id   SERIAL PRIMARY KEY,
      user_id     INT NOT NULL UNIQUE,
      balance     DECIMAL(15,2) DEFAULT 0.00 CHECK (balance >= 0),
      status      VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED')),
      FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
  );
  ```
* **Chi tiết thay đổi từng thuộc tính (Attributes)**:
  - `wallet_id`: `SERIAL` (`INT`) - Khóa chính.
  - `user_id`: `INT NOT NULL UNIQUE` - Khóa ngoại 1-1 với `users(user_id)`.
  - `balance`: Kiểu **`DECIMAL(15,2)`** (Java dùng `BigDecimal`), mặc định `0.00`, ràng buộc `CHECK (balance >= 0)`.
  - `status`: `VARCHAR(20)` với ràng buộc `CHECK (status IN ('ACTIVE','SUSPENDED'))`.
  - ❌ **ĐÃ BỎ**: `currency` (không còn cột này).
  - ❌ **ĐÃ BỎ**: `created_at`, `updated_at` (bảng `wallets` trong `cinema sql.txt` không còn 2 cột này).

---

### 1.2 Bảng `cinema.wallet_transactions`
* **Định nghĩa chuẩn trong `cinema sql.txt`**:
  ```sql
  CREATE TABLE wallet_transactions (
      transaction_id    SERIAL PRIMARY KEY,
      wallet_id         INT NOT NULL,
      amount            DECIMAL(15,2) NOT NULL CHECK (amount > 0),
      transaction_type  VARCHAR(20) NOT NULL CHECK (transaction_type IN ('ADD_MONEY','PAYMENT','REFUND')),
      status            VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('SUCCESSFUL','FAILED','PENDING')),
      description       VARCHAR(500),
      created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (wallet_id) REFERENCES wallets(wallet_id) ON DELETE RESTRICT
  );
  ```
* **Chi tiết thay đổi từng thuộc tính (Attributes)**:
  - `transaction_id`: Đổi tên từ `wallet_transaction_id` thành **`transaction_id`** (`SERIAL` / `INT`).
  - `wallet_id`: `INT NOT NULL` (Khóa ngoại trỏ đến `wallets.wallet_id`).
  - `amount`: Kiểu **`DECIMAL(15,2)`** (Java dùng `BigDecimal`), ràng buộc `CHECK (amount > 0)`.
  - `transaction_type`: Đổi giá trị nạp tiền từ `TOP_UP` thành **`ADD_MONEY`** (`ADD_MONEY`, `PAYMENT`, `REFUND`).
  - ⭐ **THUỘC TÍNH MỚI**: Thêm cột **`status`** `VARCHAR(20)` mặc định `'PENDING'`, ràng buộc `CHECK (status IN ('SUCCESSFUL','FAILED','PENDING'))`.
  - `description`: Đổi kiểu từ `TEXT` thành **`VARCHAR(500)`**.
  - `created_at`: `TIMESTAMP DEFAULT CURRENT_TIMESTAMP`.
  - ❌ **ĐÃ BỎ**: `balance_after` (không còn lưu số dư sau giao dịch).
  - ❌ **ĐÃ BỎ**: `direction` (không còn cột `IN`/`OUT` hay `CREDIT`/`DEBIT`).
  - ❌ **ĐÃ BỎ**: `currency` (không còn cột tiền tệ).
  - ❌ **ĐÃ BỎ**: `topup_id` (không còn bảng `wallet_topups`).
  - ❌ **ĐÃ BỎ**: `payment_id`, `refund_id` (quan hệ thanh toán hiện tại được lưu ở bảng `payments` với cột `payments.wallet_transaction_id` trỏ ngược về bảng này).

---

### 1.3 Bảng `WalletTopUp`
* ❌ **ĐÃ BỊ XÓA HOÀN TOÀN TRONG CSDL**:
  Trong [cinema sql.txt](file:///d:/IT1_UTE/HK5_26_27/lap_trinh_web/project/cinema_system/cinema%20sql.txt), bảng `cinema.wallet_topups` không còn tồn tại.
  Nghiệp vụ nạp ví được ghi trực tiếp vào `wallet_transactions` với `transaction_type = 'ADD_MONEY'`, và thông tin thanh toán nạp tiền được quản lý tại bảng `cinema.payments` (`payment_type = 'TOPUP'`).

---

## 2. Cấu trúc thư mục Module `wallet`

Xóa bỏ triệt để các file liên quan đến `WalletTopup` và tinh gọn cấu trúc module:

```
src/main/java/com/cinema/wallet/
├── controller/
│   └── WalletController.java          ← Servlet @WebServlet(urlPatterns = {"/wallet", "/wallet/*"})
├── service/
│   └── WalletService.java             ← Logic nghiệp vụ ví & giao dịch
├── dao/
│   ├── WalletDAO.java                 ← DAO thao tác bảng cinema.wallets
│   └── WalletTransactionDAO.java      ← DAO thao tác bảng cinema.wallet_transactions
├── entity/
│   ├── Wallet.java                    ← JPA Entity khớp bảng cinema.wallets
│   └── WalletTransaction.java         ← JPA Entity khớp bảng cinema.wallet_transactions
├── enums/
│   ├── WalletStatus.java              ← Enum: ACTIVE, SUSPENDED
│   ├── TransactionType.java           ← Enum: ADD_MONEY, PAYMENT, REFUND
│   └── TransactionStatus.java         ← Enum: SUCCESSFUL, FAILED, PENDING
├── exception/
│   └── WalletException.java           ← Ngoại lệ nghiệp vụ ví
├── dto/
│   ├── request/
│   │   └── TopUpRequest.java          ← DTO nạp tiền (amount, method)
│   └── response/
│       ├── WalletResponse.java        ← DTO thông tin ví (walletId, userId, balance, status)
│       ├── TopUpResponse.java         ← DTO phản hồi nạp tiền
│       └── TransactionItemResponse.java ← DTO chi tiết giao dịch
│       (Phân trang dùng chung CommonDTO.PageMeta từ module common)
├── docs/
│   ├── PLAN.md                        ← Kế hoạch thiết kế & triển khai này
│   └── POSTMAN_GUIDE.md               ← Hướng dẫn test Postman
└── test/
    └── wallet_postman.json            ← Postman Collection kiểm thử
```

---

## 3. Thiết kế Các Lớp Java & Entity

### 3.1 Các Enum chuẩn
```java
// com.cinema.wallet.enums.WalletStatus
public enum WalletStatus {
    ACTIVE,
    SUSPENDED
}

// com.cinema.wallet.enums.TransactionType
public enum TransactionType {
    ADD_MONEY,
    PAYMENT,
    REFUND
}

// com.cinema.wallet.enums.TransactionStatus
public enum TransactionStatus {
    SUCCESSFUL,
    FAILED,
    PENDING
}
```

---

### 3.2 Entity `Wallet.java`
Map chính xác 100% với bảng `cinema.wallets` trong `cinema sql.txt`:
```java
package com.cinema.wallet.entity;

import com.cinema.user.entity.User;
import com.cinema.wallet.enums.WalletStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "wallets", schema = "cinema")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WalletStatus status = WalletStatus.ACTIVE;

    public Wallet() {}

    public Wallet(User user) {
        this.user = user;
        this.balance = BigDecimal.ZERO;
        this.status = WalletStatus.ACTIVE;
    }

    // Getters và Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public WalletStatus getStatus() { return status; }
    public void setStatus(WalletStatus status) { this.status = status; }
}
```

---

### 3.3 Entity `WalletTransaction.java`
Map chính xác 100% với bảng `cinema.wallet_transactions` trong `cinema sql.txt`:
```java
package com.cinema.wallet.entity;

import com.cinema.wallet.enums.TransactionStatus;
import com.cinema.wallet.enums.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet_transactions", schema = "cinema")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    public WalletTransaction() {}

    // Getters và Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
```

---

### 3.4 Data Transfer Objects (DTO)
Khớp chuẩn với `swagger_cinema.yaml`:

1. **`WalletResponse`**:
   ```java
   public record WalletResponse(
       Long walletId,
       Long userId,
       BigDecimal balance,
       WalletStatus status
   ) {}
   ```

2. **`TopUpRequest`**:
   ```java
   public record TopUpRequest(
       BigDecimal amount,
       String method
   ) {}
   ```

3. **`TopUpResponse`**:
   ```java
   public record TopUpResponse(
       Long transactionId,
       BigDecimal amount,
       String method,
       String status,
       Instant createdAt
   ) {}
   ```

4. **`TransactionItemResponse`**:
   ```java
   public record TransactionItemResponse(
       Long transactionId,
       Long walletId,
       BigDecimal amount,
       TransactionType transactionType,
       TransactionStatus status,
       String description,
       Instant createdAt
   ) {}
   ```

5. **Phân trang (Pagination Response)**:
   Sử dụng trực tiếp lớp dùng chung của toàn hệ thống `com.cinema.common.dto.CommonDTO.PageMeta` (chứa `page`, `size`, `totalElements`, `totalPages`), không cần sinh thêm file riêng `PageOfWalletTransaction.java`.

---

## 4. Chi tiết Request & Response của từng Endpoint (Theo chuẩn `swagger_cinema.yaml`)

Format chuẩn của hệ thống:
- Thành công: `{ "success": true, "data": ... }` hoặc `{ "success": true, "data": ..., "meta": ... }`
- Thất bại: `{ "success": false, "status": <HTTP_CODE>, "error": "<Thông báo lỗi>" }`

---

### 4.1 Endpoint: `GET /wallet` (Xem số dư ví)
* **Quyền**: Yêu cầu đăng nhập (`BearerAuth`).
* **Headers**: `Authorization: Bearer <token>`
* **Request Body**: Không có.

#### Các trường hợp Response:
* **HTTP 200 OK (Thành công)**:
  ```json
  {
    "success": true,
    "data": {
      "walletId": 1,
      "userId": 1,
      "balance": 500000,
      "status": "ACTIVE"
    }
  }
  ```

* **HTTP 401 Unauthorized (Chưa đăng nhập / Token không hợp lệ)**:
  ```json
  {
    "success": false,
    "status": 401,
    "error": "Chua dang nhap"
  }
  ```

* **HTTP 403 Forbidden (Ví bị khóa)**:
  ```json
  {
    "success": false,
    "status": 403,
    "error": "Ví của bạn đang bị tạm khóa (SUSPENDED)"
  }
  ```

---

### 4.2 Endpoint: `GET /wallet/transactions` (Lịch sử giao dịch ví)
* **Quyền**: Yêu cầu đăng nhập (`BearerAuth`).
* **Headers**: `Authorization: Bearer <token>`
* **Query Parameters**:
  - `type` (optional, string): `ADD_MONEY` | `PAYMENT` | `REFUND`
  - `page` (optional, integer): Số trang (bắt đầu từ 0, mặc định `0`)
  - `size` (optional, integer): Số phần tử trên trang (mặc định `20`, tối đa `100`)
* **Request Body**: Không có.

#### Các trường hợp Response:
* **HTTP 200 OK (Thành công - Có phân trang `meta` theo `CommonDTO.PageMeta`)**:
  ```json
  {
    "success": true,
    "data": {
      "items": [
        {
          "transactionId": 1,
          "walletId": 1,
          "amount": 500000,
          "transactionType": "ADD_MONEY",
          "status": "SUCCESSFUL",
          "description": "Nap tien vao vi",
          "createdAt": "2024-07-15T18:30:00Z"
        }
      ],
      "meta": {
        "page": 0,
        "size": 20,
        "totalElements": 1,
        "totalPages": 1
      }
    }
  }
  ```

* **HTTP 400 Bad Request (Tham số lọc không hợp lệ)**:
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Tham so loc type, page hoac size khong hop le"
  }
  ```

* **HTTP 401 Unauthorized (Chưa đăng nhập)**:
  ```json
  {
    "success": false,
    "status": 401,
    "error": "Chua dang nhap"
  }
  ```

---

### 4.3 Endpoint: `POST /wallet/top-up` (Tạo giao dịch nạp ví)
* **Quyền**: Yêu cầu đăng nhập (`BearerAuth`).
* **Headers**:
  - `Authorization: Bearer <token>`
  - `Content-Type: application/json`
* **Request Body** (JSON - `TopUpRequest`):
  - `amount` (integer, bắt buộc): Số tiền nạp VND (từ `10000` đến `10000000`)
  - `method` (string, bắt buộc): Phương thức nạp (`QR_CODE`, `MOMO`, `VNPAY`)
  ```json
  {
    "amount": 500000,
    "method": "MOMO"
  }
  ```

#### Các trường hợp Response:
* **HTTP 201 Created (Tạo nạp tiền thành công)**:
  ```json
  {
    "success": true,
    "data": {
      "paymentId": 501,
      "bookingId": null,
      "amount": 500000,
      "method": "MOMO",
      "status": "SUCCESSFUL",
      "paymentType": "TOPUP",
      "paymentDate": "2024-07-15T18:30:00Z",
      "expiredAt": null,
      "gatewayTransactionId": null,
      "walletTransactionId": 1,
      "failureReason": null
    }
  }
  ```

* **HTTP 400 Bad Request (Dữ liệu không hợp lệ / Phương thức không hỗ trợ)**:
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Phuong thuc thanh toan khong hop le"
  }
  ```

* **HTTP 401 Unauthorized (Chưa đăng nhập)**:
  ```json
  {
    "success": false,
    "status": 401,
    "error": "Chua dang nhap"
  }
  ```

* **HTTP 403 Forbidden (Ví bị khóa SUSPENDED)**:
  ```json
  {
    "success": false,
    "status": 403,
    "error": "Ví của bạn đang bị tạm khóa (SUSPENDED), không thể nạp tiền"
  }
  ```

* **HTTP 422 Unprocessable Entity - Trường hợp 1 (Số tiền quá nhỏ < 10.000 VND)**:
  ```json
  {
    "success": false,
    "status": 422,
    "error": "So tien nap toi thieu 10.000 VND"
  }
  ```

* **HTTP 422 Unprocessable Entity - Trường hợp 2 (Số tiền quá lớn > 10.000.000 VND)**:
  ```json
  {
    "success": false,
    "status": 422,
    "error": "So tien nap toi da 10.000.000 VND"
  }
  ```

---

## 5. Kế hoạch Thực hiện Cập nhật Code Module `wallet`

Thực hiện nghiêm ngặt trong nội bộ module `wallet` theo nguyên tắc đã cam kết:

1. **Tạo Enum mới**: `enums/TransactionStatus.java` (`SUCCESSFUL`, `FAILED`, `PENDING`).
2. **Xóa 3 file thừa**:
   - `entity/WalletTopup.java`
   - `dao/WalletTopupDAO.java`
   - `enums/WalletTopupStatus.java`
3. **Cập nhật Entity**:
   - `entity/Wallet.java`: Chuyển `balance` sang `BigDecimal`, bỏ các trường không tồn tại trong `cinema sql.txt`.
   - `entity/WalletTransaction.java`: Cập nhật `transaction_id`, `amount` (`BigDecimal`), `TransactionType`, `TransactionStatus`, `description` (`VARCHAR(500)`).
4. **Cập nhật DTO**:
   - `dto/request/TopUpRequest.java`
   - `dto/response/WalletResponse.java`
   - `dto/response/TopUpResponse.java`
   - `dto/response/TransactionItemResponse.java`
   *(Phân trang tái sử dụng `CommonDTO.PageMeta` có sẵn, không tạo thêm file mới)*
5. **Cập nhật DAO & Service**:
   - `dao/WalletDAO.java` & `dao/WalletTransactionDAO.java`: Dùng `BigDecimal`, hỗ trợ filter `TransactionType`, phân trang kết hợp `PageMeta`.
   - `service/WalletService.java`: Logic cộng tiền với `BigDecimal`, ghi nhận `WalletTransaction` (`ADD_MONEY`, `SUCCESSFUL`).
6. **Cập nhật Controller**:
   - `controller/WalletController.java`: Định tuyến `/wallet/transactions`, trả về `data` và `meta` theo `CommonDTO.PageMeta`.
7. **Cập nhật Unit Test**:
   - Cập nhật các test case trong `src/test/java/com/cinema/wallet/` để đảm bảo build và chạy thành công 100%.
