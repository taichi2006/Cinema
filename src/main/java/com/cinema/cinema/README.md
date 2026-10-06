# Tài Liệu Kỹ Thuật: Module Cinema (`com.cinema.cinema`)

Tài liệu kỹ thuật mô tả kiến trúc, endpoint API, cấu trúc dữ liệu, cơ chế phân trang, tìm kiếm và giải pháp tối ưu truy vấn CSDL của module rạp chiếu phim.

---

## 1. Cấu Trúc Thành Phần

```text
com.cinema.cinema/
├── controller/
│   └── CinemaController.java             # Servlet tiếp nhận HTTP GET /cinema, điều phối URL Dispatcher
├── dao/
│   └── CinemaDAO.java                    # Tầng truy vấn CSDL JPA/Hibernate (JPQL & Native SQL)
├── dto/
│   ├── request/
│   │   └── CinemaRequest.java            # DTO đóng gói tham số query & pagination
│   └── response/
│       ├── CinemaResponse.java           # DTO dữ liệu rạp trả về client
│       └── CinemaShowtimeResponse.java   # DTO dữ liệu lịch chiếu rạp (14 trường)
├── entity/
│   └── Cinema.java                       # Entity ánh xạ bảng cinema.cinemas
├── service/
│   └── CinemaService.java                # Xử lý validation, phân trang và mapping DTO
└── README.md                             # Tài liệu kỹ thuật module
```

---

## 2. Đặc Tả API: Danh Sách Và Tìm Kiếm Rạp

- **Endpoint:** `GET /api/cinema`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/cinema", "/cinema/*"})` (chuyển tiếp qua `ApiPrefixFilter`)
- **Quyền truy cập:** Public (không yêu cầu JWT token qua `AuthFilter`)
- **Content-Type:** `application/json;charset=UTF-8`

### 2.1. Query Parameters (`CinemaRequest`)

| Tham số | Kiểu dữ liệu | Mặc định | Ràng buộc kỹ thuật |
| :--- | :--- | :---: | :--- |
| `city` | `String` | `null` | Lọc theo mã thành phố (`cityCode`, ví dụ: `HCM`, `HN`) hoặc tên thành phố (`cityName`, ví dụ: `Hồ Chí Minh`, `Hà Nội`). Không phân biệt hoa thường. |
| `q` | `String` | `null` | Từ khóa tìm kiếm theo tên rạp hoặc địa chỉ. Tối đa 100 ký tự. Vượt quá 100 ký tự $\rightarrow$ lỗi 400. |
| `sort` | `String` | `name,asc` | Thứ tự sắp xếp tên rạp. Thuộc tập enum: `name,asc`, `name,desc`. Giá trị khác $\rightarrow$ lỗi 400. |
| `page` | `int` | `0` | Chỉ số trang, $\ge 0$. Sai kiểu hoặc âm $\rightarrow$ lỗi 400. |
| `size` | `int` | `20` | Kích thước trang, $[1, 50]$. Ngoài khoảng $\rightarrow$ lỗi 400. |

*(Lưu ý: Hệ thống kiểm tra an toàn tràn số: `page * size` không được vượt quá `Integer.MAX_VALUE`).*

### 2.2. Phản Hồi Thành Công (HTTP 200 OK)

Cấu trúc flat JSON chuẩn hóa với `meta` ([`CommonDTO.PageMeta`](file:///c:/Users/khong/Desktop/TaiLieuHocTap/HK5_Nam3/WebProgramming/Cinema/src/main/java/com/cinema/common/dto/CommonDTO.java#L39)):

```json
{
  "success": true,
  "data": [
    {
      "id": "1",
      "name": "Galaxy Nguyễn Du",
      "cityCode": "HCM",
      "cityName": "Hồ Chí Minh",
      "address": "116 Nguyễn Du, Quận 1, TP.HCM",
      "phone": "028 3823 4567",
      "imageUrl": "https://example.com/cinema1.jpg",
      "latitude": 10.7725,
      "longitude": 106.698
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

- `data`: Mảng danh sách các rạp (`CinemaResponse`).
  - `id`: Mã rạp dạng chuỗi `String` theo chuẩn REST (`cinema_id`).
  - `name`: Tên rạp (`cinema_name`).
  - `cityCode`: Mã thành phố (`city_code`).
  - `cityName`: Tên thành phố (`city_name`).
  - `address`: Địa chỉ chi tiết (`address`).
  - `phone`: Số điện thoại liên hệ (có thể `null`).
  - `imageUrl`: Đường dẫn ảnh đại diện của cụm rạp (có thể `null`).
  - `latitude`, `longitude`: Tọa độ địa lý (Double, có thể `null`).
  - *(Lưu ý: Hệ thống chỉ trả về các rạp đang mở hoạt động với `status = 'ACTIVE'`).*
- `meta`: Thông tin phân trang dùng chung `com.cinema.common.dto.CommonDTO.PageMeta`.

### 2.3. Phản Hồi Lỗi (HTTP 400 / 500)

Được xử lý tập trung qua `com.cinema.common.exception.ErrorHandler`:

```json
{
  "success": false,
  "status": 400,
  "error": "Từ khóa tìm kiếm 'q' không được vượt quá 100 ký tự."
}
```

- **Từ khóa `q` vượt quá độ dài (HTTP 400 Bad Request):** Khi `q.length() > 100`.
- **Tham số `sort` không hợp lệ (HTTP 400 Bad Request):** Khi `sort` không thuộc `[name,asc, name,desc]`.
- **Phân trang không hợp lệ (HTTP 400 Bad Request):** Khi `page < 0`, `size < 1` hoặc `size > 50`.
- **Lỗi không xác định:** Trả về HTTP 500.

---

## 3. Thiết Kế Cơ Sở Dữ Liệu & Entity (`Cinema.java`)

Bảng `cinema.cinemas` trong PostgreSQL:
```sql
CREATE TABLE cinema.cinemas (
    cinema_id int8 GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cinema_name varchar(150) NOT NULL,
    address varchar(300) NOT NULL,
    city_code varchar(30) NOT NULL,
    city_name varchar(100) NOT NULL,
    phone varchar(30) NULL,
    image_url text NULL,
    latitude numeric(9, 6) NULL,
    longitude numeric(9, 6) NULL,
    status varchar(20) DEFAULT 'ACTIVE' NOT NULL
);
```

### Ánh xạ Entity JPA (`Cinema.java`):
- `@Entity` `@Table(name = "cinemas", schema = "cinema")`
- Các trường: `cinemaId` (`@Id`), `cinemaName`, `address`, `cityCode`, `cityName`, `phone`, `imageUrl`, `latitude`, `longitude`, `status`.
- Đăng ký class vào `src/main/resources/META-INF/persistence.xml`:
  ```xml
  <class>com.cinema.cinema.Cinema</class>
  ```

---

## 4. Kỹ Thuật Truy Vấn CSDL (`CinemaDAO.java`)

`CinemaDAO` sử dụng kết hợp JPQL động cho tìm kiếm rạp và Native SQL tối ưu cho truy vấn lịch chiếu:

```sql
SELECT c FROM Cinema c WHERE c.status = 'ACTIVE'
  [AND (LOWER(c.cityCode) = :city OR LOWER(c.cityName) = :city)]
  [AND (LOWER(c.cinemaName) LIKE :qPattern OR LOWER(c.address) LIKE :qPattern)]
ORDER BY c.cinemaName ASC/DESC, c.cinemaId ASC
```

### Điểm nổi bật:
1. **Lọc thành phố đa năng:** Người dùng có thể truyền mã `city=HCM` hoặc tên `city=Hồ Chí Minh` đều được nhận diện chính xác.
2. **Tìm kiếm thông minh:** Từ khóa `q` quét đồng thời trên cả tên rạp (`cinemaName`) và địa chỉ chi tiết (`address`).
3. **Sắp xếp ổn định (Stable Sort):** Luôn bổ sung khóa phụ `c.cinemaId ASC` sau `c.cinemaName` để đảm bảo thứ tự phân trang nhất quán.
4. **Truy vấn đếm tổng (`countCinemas`):**
   ```sql
   SELECT COUNT(c) FROM Cinema c WHERE c.status = 'ACTIVE' ...
   ```
   Dùng tính toán `totalPages = Math.ceil(totalElements / size)`.

---

## 5. Kiến Trúc Định Tuyến Mở Rộng (URL Dispatcher)

`CinemaController` phân tích `request.getPathInfo()` theo các phân đoạn (segments), hỗ trợ đầy đủ các endpoint theo thiết kế RESTful:

```text
GET /api/cinema
 ├── /                          ──> handleGetCinemas()             [Danh sách & tìm kiếm rạp]
 └── /{id}
      ├── (không có hậu tố)     ──> handleGetCinemaDetail(id)      [Chi tiết 1 rạp]
      └── /showtime             ──> handleGetCinemaShowtimes(id)   [Lịch chiếu của rạp theo ngày & phim]
```

---

## 6. Đặc Tả API: Chi Tiết Một Rạp

- **Endpoint:** `GET /api/cinema/{id}`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/cinema", "/cinema/*"})` (chuyển tiếp qua `ApiPrefixFilter`)
- **Quyền truy cập:** Public
- **Content-Type:** `application/json;charset=UTF-8`

### 6.1. Tham Số Yêu Cầu

| Tham số | Vị trí | Kiểu | Bắt buộc | Ràng buộc kỹ thuật |
| :--- | :--- | :--- | :---: | :--- |
| `id` | Path | `String` / `Long` | **Có** | Mã rạp. Phải là số nguyên dương $\ge 1$. Sai định dạng ném lỗi 400. Nếu rạp không tồn tại hoặc `status != 'ACTIVE'` $\rightarrow$ ném lỗi 404 (`CINEMA_NOT_FOUND`). |

### 6.2. Phản Hồi Thành Công (HTTP 200 OK)

Cấu trúc flat JSON kèm `meta: {}`:

```json
{
  "success": true,
  "data": {
    "id": "1",
    "name": "Galaxy Nguyễn Du",
    "cityCode": "HCM",
    "cityName": "Hồ Chí Minh",
    "address": "116 Nguyễn Du, Quận 1, TP.HCM",
    "phone": "028 3823 4567",
    "imageUrl": "https://example.com/cinema1.jpg",
    "latitude": 10.7725,
    "longitude": 106.698
  },
  "meta": {}
}
```

- `data`: Đối tượng `CinemaResponse` chứa thông tin chi tiết của cụm rạp.
- `meta`: Object rỗng `{}`.

### 6.3. Phản Hồi Lỗi

Xử lý tập trung qua `com.cinema.common.exception.ErrorHandler`:

- **Không tìm thấy rạp (HTTP 404 Not Found):**
  ```json
  {
    "success": false,
    "status": 404,
    "error": "Không tìm thấy rạp"
  }
  ```
- **Mã rạp không hợp lệ (HTTP 400 Bad Request):**
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Mã rạp 'id' không hợp lệ: abc"
  }
  ```

---

## 7. Đặc Tả API: Lịch Chiếu Đang Mở Bán Tại Rạp Theo Ngày Và Phim

- **Endpoint:** `GET /api/cinema/{id}/showtime`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"/cinema", "/cinema/*"})`
- **Quyền truy cập:** Public
- **Content-Type:** `application/json;charset=UTF-8`

### 7.1. Tham Số Yêu Cầu

| Tham số | Vị trí | Kiểu | Bắt buộc | Mặc định | Ràng buộc kỹ thuật |
| :--- | :--- | :--- | :---: | :---: | :--- |
| `id` | Path | `String` | **Có** | - | Mã rạp, số nguyên dương $\ge 1$. Rạp phải tồn tại và `status = 'ACTIVE'`. Không thỏa $\rightarrow$ lỗi 400 hoặc 404. |
| `date` | Query | `String` | **Có** | - | Ngày chiếu cần tra cứu theo định dạng `YYYY-MM-DD` (ISO-8601). Thiếu hoặc sai định dạng $\rightarrow$ lỗi 400. |
| `movieId` | Query | `String` | Không | `null` | Lọc suất chiếu của phim cụ thể. Phải là số nguyên dương $\ge 1$. Sai kiểu $\rightarrow$ lỗi 400. |
| `page` | Query | `int` | Không | `0` | Chỉ số trang, $\ge 0$. Sai kiểu hoặc âm $\rightarrow$ lỗi 400. |
| `size` | Query | `int` | Không | `20` | Số lượng suất chiếu trên mỗi trang, $[1, 50]$. Ngoài khoảng $\rightarrow$ lỗi 400. |

### 7.2. Phản Hồi Thành Công (HTTP 200 OK)

Cấu trúc flat JSON với đầy đủ 14 trường trong mỗi đối tượng `Showtime`:

```json
{
  "success": true,
  "data": [
    {
      "id": "101",
      "movieId": "10",
      "movieTitle": "Dune: Part Two",
      "cinemaId": "1",
      "cinemaName": "Galaxy Nguyễn Du",
      "roomId": "5",
      "roomName": "Cinema 1",
      "startsAt": "2026-10-05T18:00:00+07:00",
      "endsAt": "2026-10-05T20:30:00+07:00",
      "format": "2D",
      "language": "VietSub",
      "minTicketPrice": 85000,
      "currency": "VND",
      "availableSeatCount": 95
    }
  ],
  "meta": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### Giải thích các trường dữ liệu (`CinemaShowtimeResponse`):
- `id`: Mã suất chiếu (`showtime_id`).
- `movieId`: Mã phim (`movie_id`).
- `movieTitle`: Tên phim (`title`).
- `cinemaId`: Mã rạp (`cinema_id`).
- `cinemaName`: Tên rạp (`cinema_name`).
- `roomId`: Mã phòng chiếu (`room_id`).
- `roomName`: Tên phòng chiếu (`room_name`).
- `startsAt`: Thời gian bắt đầu chiếu (định dạng ISO-8601).
- `endsAt`: Thời gian kết thúc chiếu (định dạng ISO-8601).
- `format`: Định dạng chiếu (ví dụ: `2D`, `3D`, `IMAX`).
- `language`: Ngôn ngữ / phụ đề (ví dụ: `VietSub`, `Dub`).
- `minTicketPrice`: Giá vé thấp nhất khả dụng của suất chiếu (`Long`).
  - Ưu tiên tính từ bảng `cinema.showtime_seats` với các ghế chưa bị khóa (`MIN(ss.price) WHERE ss.is_blocked = false`).
  - Fallback về giá cơ sở `s.base_price` nếu chưa khởi tạo bảng ghế riêng.
- `currency`: Đơn vị tiền tệ (mặc định `"VND"`).
- `availableSeatCount`: Số lượng ghế còn trống có thể đặt (`Integer`).
  - Khi đã có bảng ghế riêng: Đếm số ghế không bị khóa và không nằm trong đơn đặt vé có trạng thái `HELD` hoặc `BOOKED`.
  - Khi chưa khởi tạo bảng ghế riêng: Tính từ `r.capacity - (số ghế đang HELD/BOOKED)`.
- `meta`: Phân trang chuẩn hóa [`CommonDTO.PageMeta`](file:///c:/Users/khong/Desktop/TaiLieuHocTap/HK5_Nam3/WebProgramming/Cinema/src/main/java/com/cinema/common/dto/CommonDTO.java#L39).

### 7.3. Phản Hồi Lỗi

- **Thiếu tham số bắt buộc `date` (HTTP 400 Bad Request):**
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Tham số 'date' là bắt buộc (định dạng YYYY-MM-DD)."
  }
  ```
- **Sai định dạng `date` (HTTP 400 Bad Request):**
  ```json
  {
    "success": false,
    "status": 400,
    "error": "Tham số 'date' không hợp lệ hoặc sai định dạng YYYY-MM-DD: invalid-date"
  }
  ```
- **Không tìm thấy rạp hoặc rạp không hoạt động (HTTP 404 Not Found):**
  ```json
  {
    "success": false,
    "status": 404,
    "error": "Không tìm thấy rạp"
  }
  ```

---

## 8. Kiểm Thử Đơn Vị (Unit Testing)

Tất cả các kịch bản nghiệp vụ của module `cinema` được kiểm thử tự động trong `CinemaServiceTest.java` thông qua kỹ thuật mock DAO (`StubCinemaDAO`):

- **Tổng số tests:** 22 test cases (Toàn bộ dự án: 46 test cases).
- **Tỷ lệ vượt qua:** 100% PASS (`BUILD SUCCESS`).
- **Phạm vi kiểm thử:**
  1. `testGetCinemas_Success`: Trả về danh sách rạp đầy đủ trường, kiểm tra cấu trúc JSON và PageMeta.
  2. `testGetCinemas_DefaultSort`: Khi không truyền sort, tự động gán mặc định `name,asc`.
  3. `testGetCinemas_SortDesc`: Sắp xếp theo tên giảm dần `name,desc`.
  4. `testGetCinemas_InvalidSort`: Bắt lỗi 400 khi truyền `sort` sai.
  5. `testGetCinemas_QTooLong`: Bắt lỗi 400 khi từ khóa `q > 100` ký tự.
  6. `testGetCinemas_NegativePage`: Bắt lỗi 400 khi `page < 0`.
  7. `testGetCinemas_InvalidSize`: Bắt lỗi 400 khi `size = 0` hoặc `size > 50`.
  8. `testGetCinemas_OffsetOverflow`: Bắt lỗi 400 khi phân trang vượt quá `Integer.MAX_VALUE`.
  9. `testGetCinemas_EmptyResults`: Trả về mảng rỗng `data: []`, `totalElements: 0`, `totalPages: 0`.
  10. `testGetCinemaById_Success`: Lấy chi tiết rạp thành công, kiểm tra các trường và `meta` rỗng.
  11. `testGetCinemaById_NotFound`: Bắt lỗi 404 khi rạp không tồn tại.
  12. `testGetCinemaById_InactiveCinema`: Bắt lỗi 404 khi rạp ở trạng thái `INACTIVE`.
  13. `testGetCinemaById_InvalidId`: Bắt lỗi 400 khi `id` null, trống, âm, số 0 hoặc chữ cái.
  14. `testGetCinemaShowtimes_Success`: Lấy lịch chiếu của rạp thành công, kiểm tra đủ 14 trường và phân trang.
  15. `testGetCinemaShowtimes_FilterByMovieId`: Lọc suất chiếu của rạp theo `movieId` thành công.
  16. `testGetCinemaShowtimes_CinemaNotFound`: Bắt lỗi 404 khi rạp không tồn tại.
  17. `testGetCinemaShowtimes_InactiveCinema`: Bắt lỗi 404 khi rạp ở trạng thái `INACTIVE`.
  18. `testGetCinemaShowtimes_MissingDate`: Bắt lỗi 400 khi thiếu tham số bắt buộc `date` (null hoặc trống).
  19. `testGetCinemaShowtimes_InvalidDateFormat`: Bắt lỗi 400 khi `date` sai định dạng `YYYY-MM-DD`.
  20. `testGetCinemaShowtimes_InvalidMovieId`: Bắt lỗi 400 khi `movieId` âm, số 0 hoặc chữ cái.
  21. `testGetCinemaShowtimes_InvalidPageAndSize`: Bắt lỗi 400 khi `page < 0`, `size < 1`, `size > 50` hoặc tràn số.
  22. `testGetCinemaShowtimes_EmptyResults`: Trả về mảng rỗng `data: []` kèm `totalElements: 0` khi không có suất chiếu phù hợp.
