# Tài Liệu Kỹ Thuật: Module Home (`com.cinema.home`)

Tài liệu kỹ thuật mô tả kiến trúc, endpoint API, cấu trúc dữ liệu tổng hợp trang chủ, cơ chế định tuyến và giải pháp tối ưu truy vấn CSDL của module Home.

---

## 1. Cấu Trúc Thành Phần

```text
com.cinema.home/
├── HomeController.java              # Servlet tiếp nhận HTTP GET /, /home, /home/*
├── HomeService.java                 # Aggregator Service gọi MovieService và CinemaService (trả về Map<String, Object>)
└── README.md                        # Tài liệu kỹ thuật module home
```

---

## 2. Đặc Tả API: Dữ Liệu Trang Chủ

- **Endpoint:** `GET /`
- **Servlet Mapping:** `@WebServlet(urlPatterns = {"", "/home", "/home/*"})`
- **Định tuyến:**
  - `GET /` $\rightarrow$ `HomeController`
  - `GET /home` $\rightarrow$ `HomeController`
  - `GET /api/home` $\rightarrow$ `ApiPrefixFilter` forward tới `/home`
- **Quyền truy cập:** Public (`security: []`, không yêu cầu token)
- **Content-Type:** `application/json;charset=UTF-8`

### 2.1. Phản Hồi Thành Công (HTTP 200 OK)

Cấu trúc flat JSON chuẩn hóa kèm `meta: {}`:

```json
{
  "success": true,
  "data": {
    "nowShowing": [
      {
        "id": "1",
        "title": "Dune: Hành Tinh Cát - Phần Hai",
        "posterUrl": "https://example.com/posters/dune2.jpg",
        "durationMinutes": 166,
        "releaseDate": "2026-03-01",
        "genres": ["Khoa Học Viễn Tưởng", "Phiêu Lưu"],
        "status": "NOW_SHOWING",
        "ageRating": "T16",
        "averageRating": 4.8,
        "reviewCount": 120
      }
    ],
    "comingSoon": [
      {
        "id": "2",
        "title": "Deadpool & Wolverine",
        "posterUrl": "https://example.com/posters/deadpool3.jpg",
        "durationMinutes": 128,
        "releaseDate": "2026-11-20",
        "genres": ["Hành Động", "Hài Hước"],
        "status": "COMING_SOON",
        "ageRating": "T18",
        "averageRating": 0.0,
        "reviewCount": 0
      }
    ],
    "featuredCinemas": [
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
    "genres": [
      {
        "code": "ACTION",
        "name": "Hành Động"
      }
    ],
    "cities": [
      {
        "code": "HCM",
        "name": "Hồ Chí Minh"
      }
    ]
  },
  "meta": {}
}
```

### 2.2. Chi Tiết Các Danh Mục Dữ Liệu

1. **`nowShowing` (`List<MovieResponse>`):**
   - Danh sách phim đang chiếu (`status = 'NOW_SHOWING'`), giới hạn 10 phim, sắp xếp theo ngày phát hành mới nhất (`releaseDate DESC, movieId DESC`).
   - Tái sử dụng `MovieResponse` gồm 10 trường: `id`, `title`, `posterUrl`, `durationMinutes`, `releaseDate`, `genres`, `status`, `ageRating`, `averageRating`, `reviewCount`.
2. **`comingSoon` (`List<MovieResponse>`):**
   - Danh sách phim sắp chiếu (`status = 'COMING_SOON'`), giới hạn 10 phim, sắp xếp theo ngày phát hành gần nhất (`releaseDate ASC, movieId ASC`).
3. **`featuredCinemas` (`List<CinemaResponse>`):**
   - Danh sách các cụm rạp nổi bật đang hoạt động (`status = 'ACTIVE'`), giới hạn 10 rạp, sắp xếp theo tên (`cinemaName ASC, cinemaId ASC`).
   - Tái sử dụng `CinemaResponse` gồm 9 trường: `id`, `name`, `cityCode`, `cityName`, `address`, `phone`, `imageUrl`, `latitude`, `longitude`.
4. **`genres` (`List<Map<String, String>>`):**
   - Danh sách tất cả thể loại phim từ bảng `cinema.genres`: `code` (`genre_code`), `name` (`genre_name`), sắp xếp `name ASC`.
5. **`cities` (`List<Map<String, String>>`):**
   - Danh sách các thành phố có rạp đang hoạt động: trích xuất `DISTINCT city_code, city_name` từ bảng `cinema.cinemas` (`status = 'ACTIVE'`), sắp xếp `cityName ASC`.

---

## 3. Kiến Trúc Điều Phối & Tái Sử Dụng Dịch Vụ (Aggregator Pattern)

`HomeService` đóng vai trò là một **Aggregator / Orchestrator** thuần túy, gọi trực tiếp các dịch vụ và truy vấn từ module `movie` và `cinema` mà **không tạo DAO trùng lặp**:

- **Module `movie` (`MovieService` / `MovieDAO`):**
  - Cung cấp `movieService.getMovies(MovieRequest)` để lấy `nowShowing` và `comingSoon`.
  - Cung cấp `movieService.getGenres()` để lấy toàn bộ thể loại phim (`Genre`).
- **Module `cinema` (`CinemaService` / `CinemaDAO`):**
  - Cung cấp `cinemaService.getCinemas(CinemaRequest)` để lấy `featuredCinemas`.
  - Cung cấp `cinemaService.getCities()` để lấy danh mục các thành phố (`CityItem`).
- **Lợi ích:** Loại bỏ hoàn toàn mã trùng lặp (DRY - Don't Repeat Yourself), đảm bảo logic phân trang, lọc và ánh xạ DTO nhất quán xuyên suốt ứng dụng.

---

## 4. Kiểm Thử Đơn Vị (Unit Testing)

Tất cả các kịch bản của module `home` được kiểm thử tự động trong `HomeServiceTest.java` thông qua kỹ thuật mock DAO của `Movie` và `Cinema`:

- **Tổng số tests:** 3 test cases (Toàn bộ dự án: 58 test cases).
- **Tỷ lệ vượt qua:** 100% PASS (`BUILD SUCCESS`).
- **Phạm vi kiểm thử:**
  1. `testGetHomeData_Success`: Trả về đầy đủ 5 danh mục dữ liệu, kiểm tra serialization JSON và `meta: {}`.
  2. `testGetHomeData_EmptyLists`: Khi database chưa có dữ liệu, trả về các mảng rỗng `[]` (không bị `null`).
  3. `testGetHomeData_JsonStructureOrder`: Xác nhận đúng thứ tự các trường tuần tự trong schema OpenAPI (`nowShowing`, `comingSoon`, `featuredCinemas`, `genres`, `cities`).
