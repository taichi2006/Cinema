# 📋 Kế hoạch triển khai Module `wallet`

> **Trạng thái**: Tinh gọn theo cấu trúc dự án & Đồng bộ 100% với JSON Response chuẩn của Nhóm — Sẵn sàng triển khai  
> **Tham chiếu**: `UMLClassDiagram.drawio.png` · Team API Specifications · Module `auth`, `user`, `movie` hiện có

---

## 1. Vị trí trong dự án (Cấu trúc phẳng, tinh gọn)

Không tạo thêm thư mục `dto/` riêng lẻ nhằm giữ đúng phong cách cấu trúc phẳng (`com.cinema.movie`, `com.cinema.user`, `com.cinema.auth`). Các request/response model được định nghĩa bằng Java `record` trong `WalletDTO.java`.

```
src/main/java/com/cinema/
└── wallet/
    ├── docs/
    │   └── PLAN.md                ← file kế hoạch này
    ├── Wallet.java                ← JPA Entity (bảng cinema.wallets theo chuẩn UML)
    ├── WalletTransaction.java     ← JPA Entity (bảng cinema.wallet_transactions theo chuẩn UML)
    ├── TransactionType.java       ← Enum: TOP_UP | PAYMENT | REFUND
    ├── TransactionStatus.java     ← Enum: PENDING | SUCCESSFUL | FAILED
    ├── WalletStatus.java          ← Enum: ACTIVE | SUSPENDED
    ├── WalletDTO.java             ← Các Java record Request & Response chuẩn theo contract nhóm
    ├── WalletDAO.java             ← Truy vấn CSDL cho Wallet
    ├── WalletTransactionDAO.java  ← Truy vấn CSDL cho WalletTransaction
    ├── WalletService.java         ← Logic nghiệp vụ & Data Mapping
    └── WalletController.java      ← Servlet @WebServlet("/wallet/*")
```

---

## 2. Thiết kế cơ sở dữ liệu (Tinh gọn, đúng chuẩn UML)

CSDL chỉ lưu trữ dữ liệu cốt lõi, không nhồi nhét các trường hiển thị không cần thiết. Các trường hiển thị như `currency`, `direction`, `id` (dạng chuỗi), `checkoutUrl`... sẽ do tầng Service/DTO đảm nhiệm.

### Bảng `cinema.wallets`

| Cột | Kiểu | Ràng buộc | Ghi chú |
|-----|------|-----------|---------|
| `wallet_id` | `SERIAL` | PK | Khóa chính tự tăng |
| `user_id` | `INT` | FK → `users.user_id`, UNIQUE, NOT NULL | Mỗi user có đúng 1 ví |
| `balance` | `NUMERIC(15,2)` | NOT NULL, DEFAULT 0.00, CHECK ≥ 0 | Sử dụng BigDecimal tránh sai số tài chính |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `'ACTIVE'` | Enum `WalletStatus`: `ACTIVE` \| `SUSPENDED` |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT `now()` | |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT `now()` | Tự cập nhật khi số dư thay đổi |

**SQL tạo bảng**:
```sql
CREATE TABLE cinema.wallets (
    wallet_id   SERIAL PRIMARY KEY,
    user_id     INT            NOT NULL UNIQUE REFERENCES cinema.users(user_id),
    balance     NUMERIC(15,2)  NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);
```

---

### Bảng `cinema.wallet_transactions`

| Cột | Kiểu | Ràng buộc | Ghi chú |
|-----|------|-----------|---------|
| `transaction_id` | `SERIAL` | PK | Khóa chính tự tăng |
| `wallet_id` | `INT` | FK → `wallets.wallet_id`, NOT NULL | Ví liên quan |
| `amount` | `NUMERIC(15,2)` | NOT NULL, CHECK > 0 | Số tiền giao dịch (luôn dương) |
| `balance_after` | `NUMERIC(15,2)` | NULLABLE | Số dư ví ngay sau khi hoàn tất bút toán |
| `transaction_type` | `VARCHAR(20)` | NOT NULL | Enum: `TOP_UP`, `PAYMENT`, `REFUND` |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT `'PENDING'` | Enum: `PENDING`, `SUCCESSFUL`, `FAILED` |
| `reference_id` | `VARCHAR(100)` | NULLABLE | Mã booking hoặc mã giao dịch ngân hàng ngoài |
| `description` | `TEXT` | NULLABLE | Ghi chú / mô tả nội dung giao dịch |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT `now()` | Thời điểm tạo giao dịch |

**SQL tạo bảng**:
```sql
CREATE TABLE cinema.wallet_transactions (
    transaction_id   SERIAL PRIMARY KEY,
    wallet_id        INT            NOT NULL REFERENCES cinema.wallets(wallet_id),
    amount           NUMERIC(15,2)  NOT NULL CHECK (amount > 0),
    balance_after    NUMERIC(15,2),
    transaction_type VARCHAR(20)    NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    reference_id     VARCHAR(100),
    description      TEXT,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now()
);

-- Index tối ưu truy vấn
CREATE INDEX idx_wallet_tx_wallet_id ON cinema.wallet_transactions(wallet_id);
CREATE INDEX idx_wallet_tx_status ON cinema.wallet_transactions(status);
CREATE INDEX idx_wallet_tx_created_at ON cinema.wallet_transactions(created_at DESC);
```

---

### Quan hệ thực thể
```
users (1) ──────── (1) wallets
wallets (1) ─────── (0..*) wallet_transactions
```

---

## 3. Các lớp Java & Kiến trúc

### 3.1 Các Enum
* **`WalletStatus`**: `ACTIVE`, `SUSPENDED`
* **`TransactionType`**: `TOP_UP`, `PAYMENT`, `REFUND`
* **`TransactionStatus`**: `PENDING`, `SUCCESSFUL`, `FAILED`

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

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
- `findHistory(long walletId, TransactionType type, Instant from, Instant to, int page, int size)`: Lấy lịch sử giao dịch đã thành công (`SUCCESSFUL`), hỗ trợ lọc theo loại và khoảng thời gian.
- `countHistory(long walletId, TransactionType type, Instant from, Instant to)`: Đếm tổng số giao dịch thỏa điều kiện.
- `findAllPending(int page, int size)`: Lấy danh sách giao dịch `PENDING` cho Admin.
- `countAllPending()`: Đếm tổng số giao dịch `PENDING`.

---

### 3.6 Nghiệp vụ `WalletService.java`

| Phương thức | Nghiệp vụ chi tiết |
|---|---|
| `createWalletForUser(User user, EntityManager em)` | Khởi tạo ví mới với `balance = 0.00`, `status = ACTIVE` khi đăng ký tài khoản. |
| `getMyWallet(long userId)` | Lấy thông tin ví của user. Nếu user cũ chưa có ví trong DB, hệ thống sẽ tự động khởi tạo ví (auto-provision) cho user đó. |
| `topUp(long userId, TopUpRequest req, String idempotencyKey)` | 1. Kiểm tra trạng thái ví: Nếu `SUSPENDED` → ném lỗi `WALLET_SUSPENDED` (HTTP 403).<br>2. Validate `amount > 0`: Nếu sai ném `TOP_UP_AMOUNT_INVALID` (HTTP 422).<br>3. Tạo `WalletTransaction` với `transactionType = TOP_UP`, `status = PENDING`.<br>4. Trả về `TopUpResponse` (chưa cộng tiền). |
| `getTopUpStatus(long userId, long txId)` | Lấy chi tiết trạng thái nạp tiền. Đảm bảo giao dịch thuộc đúng ví của `userId` hiện tại. |
| `getTransactionHistory(long userId, String typeStr, String fromDate, String toDate, int page, int size)` | Lấy danh sách bút toán `SUCCESSFUL` của ví, hỗ trợ lọc theo `type`, khoảng ngày, kèm metadata phân trang. |
| `getPendingTopUps(int page, int size)` | *(Role ADMIN)* Lấy danh sách các yêu cầu nạp tiền `PENDING`. |
| `confirmTopUp(long txId, boolean approve, String adminNote)` | *(Role ADMIN)*<br>1. Kiểm tra transaction có tồn tại và đang ở trạng thái `PENDING` không.<br>2. Nếu **approve = true**: Chuyển `status = SUCCESSFUL`, tính `balanceAfter = currentBalance + amount`, cập nhật `balance` của ví.<br>3. Nếu **approve = false**: Chuyển `status = FAILED`. |

---

### 3.7 Bộ điều khiển `WalletController.java` (`@WebServlet("/wallet/*")`)

Được bảo vệ bởi `AuthFilter`. Sinh `traceId` tự động cho mỗi request.

| HTTP Method | Path Pattern | Quyền | Phương thức xử lý | Mô tả |
|:---|:---|:---:|:---|:---|
| `GET` | `/wallet` | USER / ADMIN | `getMyWallet()` | Xem thông tin và số dư ví của tài khoản đang đăng nhập |
| `POST` | `/wallet/top-up` | USER / ADMIN | `topUp()` | Tạo yêu cầu nạp tiền (`PENDING`) |
| `GET` | `/wallet/top-up/{id}` | USER / ADMIN | `getTopUpStatus()` | Theo dõi kết quả nạp tiền của giao dịch `{id}` |
| `GET` | `/wallet/transaction` | USER / ADMIN | `getTransactionHistory()` | Xem lịch sử biến động số dư đã hoàn tất (`SUCCESSFUL`) |
| `GET` | `/wallet/top-up/pending` | **ADMIN** | `getPendingTopUps()` | Admin xem danh sách các yêu cầu nạp tiền chờ duyệt |
| `POST` | `/wallet/top-up/{id}/confirm` | **ADMIN** | `confirmTopUp()` | Admin duyệt/từ chối yêu cầu nạp tiền và cộng tiền vào ví |

---

## 4. Chi tiết Request / Response API (Chuẩn Nhóm)

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

### (Thêm) 4.5 `GET /wallet/top-up/pending` *(Role ADMIN)*
* **Quyền**: Chỉ `ADMIN` (User thường trả về `403 Forbidden`).
* **Query Params**: `page` (mặc định 0), `size` (mặc định 20).
* **Response 200 OK**:
```json
{
  "success": true,
  "data": [
    {
      "id": "101",
      "walletId": 1,
      "userId": 5,
      "userEmail": "user@example.com",
      "amount": 100000.00,
      "currency": "VND",
      "status": "PENDING",
      "description": "Nạp tiền xem phim cuối tuần",
      "createdAt": "2026-10-04T08:15:00Z"
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

---

### (Thêm) 4.6 `POST /wallet/top-up/{id}/confirm` *(Role ADMIN)*
* **Quyền**: Chỉ `ADMIN`.
* **Request Body**:
```json
{
  "approve": true,
  "note": "Đã nhận chuyển khoản ngân hàng thành công"
}
```
* **Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "id": "101",
    "walletId": 1,
    "amount": 100000.00,
    "currency": "VND",
    "status": "SUCCESSFUL",
    "newBalance": 250000.00
  },
  "meta": {},
  "traceId": "string"
}
```

---

## 5. Danh sách thay đổi ở các file hiện có

| STT | Tệp tin | Vị trí / Nội dung thay đổi |
|:---:|:---|:---|
| 1 | `AuthFilter.java` | Bổ sung `"/wallet/*"` vào `@WebFilter(urlPatterns = {..., "/wallet/*"})` |
| 2 | `persistence.xml` | Khai báo 2 Entity: `<class>com.cinema.wallet.Wallet</class>` và `<class>com.cinema.wallet.WalletTransaction</class>` |
| 3 | `AuthService.java` | Tại hàm `register(...)`, sau khi `em.persist(u)` tiến hành tạo sẵn ví rỗng cho user mới |

---

## 6. Tổng hợp các quyết định đã chốt (Q1 → Q5)

| Câu hỏi | Quyết định thống nhất |
|:---|:---|
| **Q1. Tự động tạo ví** | **CÓ**: Tự động tạo ví rỗng (`balance = 0.00`, `status = ACTIVE`) khi đăng ký tài khoản. Trong `WalletService.getMyWallet()` có cơ chế dự phòng tự tạo ví nếu user cũ chưa có. |
| **Q2. Luồng nạp tiền** | **HƯỚNG B**: User tạo yêu cầu `PENDING`. Admin kiểm tra đối soát và duyệt qua API `/confirm` để cộng tiền vào ví. |
| **Q3. Endpoint cho Admin** | **CÓ**: Bổ sung `GET /wallet/top-up/pending` và `POST /wallet/top-up/{id}/confirm` được bảo vệ bởi role `ADMIN`. |
| **Q4. Kiểu dữ liệu tiền tệ** | Sử dụng **`NUMERIC(15,2)`** trong PostgreSQL và **`BigDecimal`** trong Java Entity/Service để đảm bảo độ chính xác tuyệt đối. |
| **Q5. Xử lý ví `SUSPENDED`** | **Chặn các API giao dịch của ví**: Nếu ví đang bị tạm khóa (`SUSPENDED`), chặn nạp tiền (`POST /wallet/top-up`) và thanh toán vé (trả về lỗi `403 Forbidden` với mã `WALLET_SUSPENDED`). Cho phép xem thông tin ví (`GET /wallet`). |
