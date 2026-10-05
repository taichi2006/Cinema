# 📋 Kế hoạch triển khai Module `wallet`

> 


## 1. Vị trí trong dự án

Tổ chức thư mục `dto/` được phân tách khoa học thành 3 phần rõ ràng:
1. `envelope/`: Các lớp Envelope chuẩn Swagger (`SuccessEnvelope`, `ErrorEnvelope`, `PageMeta`, `FieldError`)
2. `request/`: Các lớp DTO đầu vào (`TopUpRequest`)
3. `response/`: Các lớp DTO đầu ra (`WalletResponse`, `TopUpResponse`, `TransactionItemResponse`)

```
src/main/java/com/cinema/
└── wallet/
    ├── docs/
    │   └── PLAN.md                ← file kế hoạch này
    ├── dto/
    │   ├── request/               ← 1. Request DTOs
    │   │   └── TopUpRequest.java
    │   └── response/              ← 2. Response DTOs
    │       ├── WalletResponse.java
    │       ├── TopUpResponse.java
    │       └── TransactionItemResponse.java
    │   (Tái sử dụng CommonDTO.ApiResponse & CommonDTO.PageMeta từ module common)
    ├── Wallet.java                ← JPA Entity (bảng cinema.wallets)
    ├── WalletTopup.java           ← JPA Entity (bảng cinema.wallet_topups)
    ├── WalletTransaction.java     ← JPA Entity (bảng cinema.wallet_transactions)
    ├── TransactionType.java       ← Enum: TOP_UP | PAYMENT | REFUND
    ├── WalletTopupStatus.java     ← Enum: PENDING | SUCCEEDED | FAILED | EXPIRED | REVERSAL_PENDING | REVERSED
    ├── WalletStatus.java          ← Enum: ACTIVE | SUSPENDED
    ├── WalletException.java       ← Exception nghiệp vụ ví kế thừa ApiException
    ├── WalletDAO.java             ← Truy vấn CSDL cho Wallet
    ├── WalletTopupDAO.java        ← Truy vấn CSDL cho WalletTopup
    ├── WalletTransactionDAO.java  ← Truy vấn CSDL cho WalletTransaction
    ├── WalletService.java         ← Logic nghiệp vụ & Data Mapping
    └── WalletController.java      ← Servlet @WebServlet(urlPatterns = {"/wallet", "/wallet/*"})
```

---

## 2. Thiết kế cơ sở dữ liệu (Khớp 100% với PostgreSQL Neon)

### Bảng `cinema.wallets`

| Cột | Kiểu | Ràng buộc | Ghi chú |
|-----|------|-----------|---------|
| `wallet_id` | `int8` | PK, Identity | Khóa chính |
| `user_id` | `int8` | FK → `users.user_id`, UNIQUE, NOT NULL | Mỗi user có đúng 1 ví |
| `balance` | `int8` | NOT NULL, DEFAULT 0 | Số dư nguyên VND (Long) |
| `currency` | `bpchar(3)` | NOT NULL, DEFAULT `'VND'` | Đơn vị tiền tệ |
| `status` | `varchar(20)` | NOT NULL, DEFAULT `'ACTIVE'` | `ACTIVE` \| `SUSPENDED` |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` | |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT `now()` | |

---

### Bảng `cinema.wallet_topups`

| Cột | Kiểu | Ràng buộc | Ghi chú |
|-----|------|-----------|---------|
| `topup_id` | `int8` | PK, Identity | Khóa chính |
| `wallet_id` | `int8` | FK → `wallets.wallet_id`, NOT NULL | Ví liên quan |
| `amount` | `int8` | NOT NULL, CHECK > 0 | Số tiền nạp VND |
| `currency` | `bpchar(3)` | NOT NULL, DEFAULT `'VND'` | |
| `status` | `varchar(25)` | NOT NULL, DEFAULT `'PENDING'` | `PENDING`, `SUCCESSFUL`, `FAILED`, `EXPIRED` |
| `checkout_url` | `text` | NULLABLE | URL thanh toán |
| `failure_code` | `varchar(80)` | NULLABLE | Mã lỗi nếu thất bại |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` | |
| `expires_at` | `timestamptz` | NULLABLE | Thời điểm hết hạn (sau 15 phút) |
| `completed_at` | `timestamptz` | NULLABLE | Thời điểm hoàn tất/duyệt |

---

### Bảng `cinema.wallet_transactions` (Sổ cái Ledger)

| Cột | Kiểu | Ràng buộc | Ghi chú |
|-----|------|-----------|---------|
| `wallet_transaction_id` | `int8` | PK, Identity | Khóa chính |
| `wallet_id` | `int8` | FK → `wallets.wallet_id`, NOT NULL | Ví liên quan |
| `transaction_type` | `varchar(10)` | NOT NULL | `TOP_UP`, `PAYMENT`, `REFUND` |
| `direction` | `varchar(6)` | NOT NULL | `IN` (cộng tiền) \| `OUT` (trừ tiền) |
| `amount` | `int8` | NOT NULL, CHECK > 0 | Số tiền biến động VND |
| `balance_after` | `int8` | NOT NULL | Số dư ví ngay sau giao dịch |
| `currency` | `bpchar(3)` | NOT NULL, DEFAULT `'VND'` | |
| `topup_id` | `int8` | NULLABLE | Khóa liên kết bảng `wallet_topups` |
| `payment_id` | `int8` | NULLABLE | Khóa liên kết bảng `payments` |
| `refund_id` | `int8` | NULLABLE | Khóa liên kết bảng `refunds` |
| `description` | `text` | NULLABLE | Ghi chú biến động số dư |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT `now()` | |

---

### Quan hệ thực thể
```
users (1) ──────── (1) wallets
wallets (1) ─────── (0..*) wallet_topups
wallets (1) ─────── (0..*) wallet_transactions
wallet_topups (1) ─ (0..1) wallet_transactions (qua topup_id)
```

---

## 3. Các lớp Java & Kiến trúc

### 3.1 Các Enum
* **`WalletStatus`**: `ACTIVE`, `SUSPENDED`
* **`TransactionType`**: `TOP_UP`, `PAYMENT`, `REFUND`
* **`WalletTopupStatus`**: `PENDING`, `SUCCEEDED`, `FAILED`, `EXPIRED`, `REVERSAL_PENDING`, `REVERSED`

---

### 3.2 Entity `Wallet.java`
```java
@Entity
@Table(name = "wallets", schema = "cinema")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long walletId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WalletStatus status = WalletStatus.ACTIVE;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;
    // Getters, Setters, Constructors
}
```

---

### 3.3 Entity `WalletTransaction.java`
```java
@Entity
@Table(name = "wallet_transactions", schema = "cinema")
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false, length = 6)
    private String direction; // "CREDIT" hoặc "DEBIT"

    @Column(name = "topup_id")
    private Long topupId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "refund_id")
    private Long refundId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    // Getters, Setters, Constructors
}
```

---

### 3.4 Data Transfer Objects (`WalletDTO.java`)

Sử dụng Java `record` tinh gọn để map giữa Entity và JSON response chuẩn của nhóm:

```java
public class WalletDTO {
    // Response cho GET /wallet
    public record WalletResponse(String id, BigDecimal balance, String currency, Instant updatedAt) {}

    // Request cho POST /wallet/top-up
    public record TopUpRequest(BigDecimal amount, String method) {}

    // Response cho POST /wallet/top-up và GET /wallet/top-up/{id}
    public record TopUpResponse(
        String id,
        BigDecimal amount,
        String currency,
        String method,
        String status,
        String checkoutUrl,
        Instant expiresAt,
        Instant createdAt,
        Instant completedAt,
        String failureCode
    ) {}

    // Response cho từng item trong GET /wallet/transaction
    public record TransactionItemResponse(
        String id,
        String type,
        String direction,       // Suy diễn: TOP_UP / REFUND -> "CREDIT", PAYMENT -> "DEBIT"
        BigDecimal amount,
        BigDecimal balanceAfter,
        String currency,        // Mặc định: "VND"
        String referenceType,   // Suy diễn từ transaction_type
        String referenceId,
        String description,
        Instant createdAt
    ) {}

    // Request cho POST /wallet/top-up/{id}/confirm (Admin)
    public record AdminConfirmRequest(boolean approve, String note) {}

    // Response cho GET /wallet/top-up/pending (Admin)
    public record AdminPendingItemResponse(
        String id,
        Long walletId,
        Long userId,
        String userEmail,
        BigDecimal amount,
        String currency,
        String status,
        String description,
        Instant createdAt
    ) {}

    // Response cho POST /wallet/top-up/{id}/confirm (Admin)
    public record AdminConfirmResponse(
        String id,
        Long walletId,
        BigDecimal amount,
        String currency,
        String status,
        BigDecimal newBalance
    ) {}
}
```

**Nguyên tắc chuyển đổi (Mapping Rules)**:
* `id`: Chuyển từ `Long` sang chuỗi `String.valueOf(id)`.
* `currency`: Mặc định luôn là `"VND"`.
* `direction`: Nếu `transactionType` là `TOP_UP` hoặc `REFUND` → `"CREDIT"`; nếu là `PAYMENT` → `"DEBIT"`.
* `checkoutUrl`: Sinh chuỗi `"/wallet/top-up/" + id`.
* `expiresAt`: Tính bằng `createdAt + 15 phút`.

---

### 3.5 Data Access Object (DAO)

**`WalletDAO`**:
- `findByUserId(long userId)`: Tìm ví theo `userId`.
- `findById(long walletId)`: Tìm ví theo `walletId`.
- `save(Wallet wallet)`: Lưu ví mới hoặc cập nhật.
- `updateBalance(long walletId, BigDecimal newBalance)`: Cập nhật số dư và `updated_at`.

**`WalletTransactionDAO`**:
- `save(WalletTransaction tx)`: Lưu bản ghi giao dịch.
- `findById(long id)`: Tìm giao dịch theo `transactionId`.
- `findHistory(long walletId, String type, Instant from, Instant to, int page, int size)`: Lấy lịch sử giao dịch, hỗ trợ lọc theo loại và khoảng thời gian.
- `countHistory(long walletId, String type, Instant from, Instant to)`: Đếm tổng số giao dịch thỏa điều kiện.

---

### 3.6 Nghiệp vụ `WalletService.java`

| Phương thức | Nghiệp vụ chi tiết |
|---|---|
| `createWalletForUser(User user, EntityManager em)` | Khởi tạo ví mới với `balance = 0`, `status = ACTIVE` khi đăng ký tài khoản. |
| `getMyWallet(long userId)` | Lấy thông tin ví của user. Nếu user cũ chưa có ví trong DB, hệ thống sẽ tự động khởi tạo ví (auto-provision) cho user đó. |
| `topUp(long userId, TopUpRequest req, String idempotencyKey)` | 1. Kiểm tra trạng thái ví: Nếu `SUSPENDED` → ném lỗi `WALLET_SUSPENDED` (HTTP 403).<br>2. Validate `10,000 <= amount <= 50,000,000 VND`.<br>3. Trong 1 transaction duy nhất: Lưu bản ghi `wallet_topups` ở trạng thái `SUCCEEDED`, tự động cộng tiền vào ví `wallets` và ghi nhận bút toán `TOP_UP` (direction = `CREDIT`) vào sổ cái `wallet_transactions`.<br>4. Trả về `TopUpResponse` (nạp thành công ngay lập tức). |
| `getTopUpStatus(long userId, long topupId)` | Lấy chi tiết trạng thái nạp tiền từ bảng `wallet_topups`. Đảm bảo giao dịch thuộc đúng ví của `userId` hiện tại. |
| `getTransactionHistory(long userId, String typeStr, String fromDate, String toDate, int page, int size)` | Lấy danh sách bút toán của ví từ `wallet_transactions`, hỗ trợ lọc theo `type`, khoảng ngày, kèm metadata phân trang. |

---

### 3.7 Bộ điều khiển `WalletController.java` (`@WebServlet("/wallet/*")`)

Được bảo vệ bởi `AuthFilter`. Sinh `traceId` tự động cho mỗi request.

| HTTP Method | Path Pattern | Quyền | Phương thức xử lý | Mô tả |
|:---|:---|:---:|:---|:---|
| `GET` | `/wallet` | USER / ADMIN | `getMyWallet()` | Xem thông tin và số dư ví của tài khoản đang đăng nhập |
| `POST` | `/wallet/top-up` | USER / ADMIN | `topUp()` | Nạp tiền vào ví (tự động nạp thành công ngay lập tức) |
| `GET` | `/wallet/top-up/{id}` | USER / ADMIN | `getTopUpStatus()` | Theo dõi kết quả nạp tiền của giao dịch `{id}` |
| `GET` | `/wallet/transaction` | USER / ADMIN | `getTransactionHistory()` | Xem lịch sử biến động số dư đã hoàn tất |

---

## 4. Chi tiết Request / Response API 

### 4.1 `GET /wallet`
* **Quyền**: Mọi user đã đăng nhập.
* **Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "id": "1",
    "balance": 150000.00,
    "currency": "VND",
    "updatedAt": "2026-10-04T09:08:42.095Z"
  },
  "meta": {},
  "traceId": "string"
}
```

* **Response 401 UNAUTHORIZED/TOKEN_EXPIRED**:
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Thiếu token xác thực"
  },
  "traceId": "req-abc-123"
}
```

---

### 4.2 `POST /wallet/top-up`
* **Quyền**: User sở hữu ví (Ví không bị `SUSPENDED`).
* **Parameters**:
  - `idempotencyKey` (header): `string` (Key ngẫu nhiên 16-128 ký tự để ngăn chặn tạo trùng giao dịch)

* **Request Body**:
```json
{
  "amount": 100000.00,
  "method": "GATEWAY"
}
```

* **Response 201 Created**:
```json
{
  "success": true,
  "data": {
    "id": "101",
    "amount": 100000.00,
    "currency": "VND",
    "method": "GATEWAY",
    "status": "PENDING",
    "checkoutUrl": "/wallet/top-up/101",
    "expiresAt": "2026-10-04T09:23:42.102Z",
    "createdAt": "2026-10-04T09:08:42.102Z",
    "completedAt": null,
    "failureCode": null
  },
  "meta": {},
  "traceId": "string"
}
```

* **Lỗi 400 `IDEMPOTENCY_KEY_REQUIRED`**:
```json
{
  "success": false,
  "error": {
    "code": "IDEMPOTENCY_KEY_REQUIRED",
    "message": "Thiếu header Idempotency-Key",
    "fieldErrors": [
      {
        "field": "idempotencyKey",
        "message": "Header idempotencyKey là bắt buộc (16-128 ký tự)"
      }
    ],
    "details": {}
  },
  "traceId": "string"
}
```

* **Lỗi 401 `UNAUTHORIZED`**:
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Thiếu token xác thực"
  },
  "traceId": "req-abc-123"
}
```

* **Lỗi 403 `WALLET_SUSPENDED`** *(nếu ví bị khóa)*:
```json
{
  "success": false,
  "error": {
    "code": "WALLET_SUSPENDED",
    "message": "Ví của bạn đang bị tạm khóa (SUSPENDED), không thể thực hiện giao dịch",
    "fieldErrors": [],
    "details": {}
  },
  "traceId": "string"
}
```

* **Lỗi 422 `TOP_UP_AMOUNT_INVALID`**:
```json
{
  "success": false,
  "error": {
    "code": "TOP_UP_AMOUNT_INVALID",
    "message": "Số tiền nạp không hợp lệ (phải lớn hơn 0)",
    "fieldErrors": [
      {
        "field": "amount",
        "message": "Số tiền nạp tối thiểu là 10,000 VND"
      }
    ],
    "details": {}
  },
  "traceId": "string"
}
```

* **Lỗi 503 `PAYMENT_PROVIDER_UNAVAILABLE`**:
```json
{
  "success": false,
  "error": {
    "code": "PAYMENT_PROVIDER_UNAVAILABLE",
    "message": "Cổng thanh toán tạm thời không khả dụng, vui lòng thử lại sau",
    "fieldErrors": [],
    "details": {}
  },
  "traceId": "string"
}
```

---

### 4.3 `GET /wallet/top-up/{id}`
* **Quyền**: Chủ ví sở hữu giao dịch.
* **Parameters**:
  - `id` (path): `string` (ID của giao dịch)

* **Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "id": "101",
    "amount": 100000.00,
    "currency": "VND",
    "method": "GATEWAY",
    "status": "PENDING",
    "checkoutUrl": "/wallet/top-up/101",
    "expiresAt": "2026-10-04T09:23:42.102Z",
    "createdAt": "2026-10-04T09:08:42.102Z",
    "completedAt": null,
    "failureCode": null
  },
  "meta": {},
  "traceId": "string"
}
```

* **Response 401 `UNAUTHORIZED`**:
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Thiếu token xác thực"
  },
  "traceId": "req-abc-123"
}
```

* **Response 404 `RESOURCE_NOT_FOUND`**:
```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Không tìm thấy giao dịch nạp tiền với ID đã cung cấp",
    "fieldErrors": [],
    "details": {}
  },
  "traceId": "string"
}
```

---

### 4.4 `GET /wallet/transaction`
* **Quyền**: Chủ ví.
* **Parameters**:
  - `type` (query): `string` (Available values: `TOP_UP`, `PAYMENT`, `REFUND`)
  - `from` (query): `string($date)` (Định dạng YYYY-MM-DD)
  - `to` (query): `string($date)` (Định dạng YYYY-MM-DD)
  - `page` (query): `integer` (0-based, mặc định là 0)
  - `size` (query): `integer` (Mặc định là 20)

* **Response 200 OK**:
```json
{
  "success": true,
  "data": [
    {
      "id": "98",
      "type": "TOP_UP",
      "direction": "CREDIT",
      "amount": 100000.00,
      "balanceAfter": 150000.00,
      "currency": "VND",
      "referenceType": "TOP_UP",
      "referenceId": "101",
      "description": "Nạp tiền ví qua chuyển khoản",
      "createdAt": "2026-10-04T09:17:32.374Z"
    }
  ],
  "meta": {
    "page": 0, 
    "size": 20, 
    "totalElements": 1,
    "totalPages": 1
  },
  "traceId": "string"
}
```

* **Response 400 `INVALID_FILTER`**:
```json
{
  "success": false,
  "error": {
    "code": "INVALID_FILTER",
    "message": "Tham số lọc ngày hoặc phân trang không hợp lệ",
    "fieldErrors": [
      {
        "field": "from",
        "message": "Ngày bắt đầu không được lớn hơn ngày kết thúc"
      }
    ],
    "details": {}
  },
  "traceId": "string"
}
```

* **Response 401 `UNAUTHORIZED`**:
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Thiếu token xác thực"
  },
  "traceId": "req-abc-123"
}
```

---

---

## 5. Danh sách thay đổi ở các file hiện có

| STT | Tệp tin | Vị trí / Nội dung thay đổi |
|:---:|:---|:---|
| 1 | `AuthFilter.java` | Bổ sung `"/wallet/*"` vào `@WebFilter(urlPatterns = {..., "/wallet/*"})` |
| 2 | `persistence.xml` | Khai báo 2 Entity: `<class>com.cinema.wallet.Wallet</class>` và `<class>com.cinema.wallet.WalletTransaction</class>` |
| 3 | `AuthService.java` | Tại hàm `register(...)`, sau khi `em.persist(u)` tiến hành tạo sẵn ví rỗng cho user mới |

---
