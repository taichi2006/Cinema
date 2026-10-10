# Tài Liệu Kỹ Thuật: Module Home (`com.cinema.home`)

Tài liệu kỹ thuật mô tả kiến trúc, endpoint API, cấu trúc dữ liệu tổng hợp trang chủ, cơ chế định tuyến và giải pháp tối ưu truy vấn CSDL của module Home.

---

## 1. Cấu Trúc Thành Phần

```text
com.cinema.home/
├── HomeController.java              # Servlet tiếp nhận HTTP GET /, /home, /home/*
├── HomeService.java                 # Aggregator Service gọi MovieService và CinemaService
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
        "movieId": 1,
        "title": "Dune: Hành Tinh Cát - Phần Hai",
        "directorId": 1,
        "durationMinutes": 166,
        "ageLimit": 16,
        "format": "2D",
        "description": "Mô tả Dune",
        "language": "VI",
        "posterUrl": "https://example.com/posters/dune2.jpg",
        "releaseDate": "2026-03-01",
        "status": "ACTIVE"
      }
    ],
    "comingSoon": [
      {
        "movieId": 2,
        "title": "Deadpool & Wolverine",
        "directorId": 2,
        "durationMinutes": 128,
        "ageLimit": 18,
        "format": "2D",
        "description": "Mô tả Deadpool",
        "language": "VI",
        "posterUrl": "https://example.com/posters/deadpool3.jpg",
        "releaseDate": "2026-11-20",
        "status": "COMING_SOON"
      }
    ],
    "featuredCinemas": [
      {
        "cinemaId": 1,
        "cinemaName": "Galaxy Nguyễn Du",
        "address": "116 Nguyễn Du, Quận 1, TP.HCM",
        "city": "Hồ Chí Minh",
        "phone": "028 3823 4567",
        "email": "contact@galaxy.vn",
        "status": "ACTIVE"
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
        "code": "Hồ Chí Minh",
        "name": "Hồ Chí Minh"
      }
    ]
  },
  "meta": {}
}
```

### 2.2. Chi Tiết Các Danh Mục Dữ Liệu

1. **`nowShowing` (`List<MovieResponse>`):**
   - Danh sách phim đang chiếu (`status = 'ACTIVE'`), giới hạn 10 phim, sắp xếp theo ngày phát hành mới nhất (`releaseDate DESC, movieId DESC`).
   - Tái sử dụng `MovieResponse` gồm 11 trường: `movieId`, `title`, `directorId`, `durationMinutes`, `ageLimit`, `format`, `description`, `language`, `posterUrl`, `releaseDate`, `status`.
2. **`comingSoon` (`List<MovieResponse>`):**
   - Danh sách phim sắp chiếu (`status = 'COMING_SOON'`), giới hạn 10 phim, sắp xếp theo ngày phát hành gần nhất (`releaseDate ASC, movieId ASC`).
3. **`featuredCinemas` (`List<CinemaResponse>`):**
   - Danh sách các cụm rạp nổi bật đang hoạt động (`status = 'ACTIVE'`), giới hạn 10 rạp, sắp xếp theo tên (`cinemaName ASC, cinemaId ASC`).
   - Tái sử dụng `CinemaResponse` gồm 7 trường: `cinemaId`, `cinemaName`, `address`, `city`, `phone`, `email`, `status`.
4. **`genres` (`List<Map<String, String>>`):**
   - Danh sách tất cả thể loại phim: `code`, `name`.
5. **`cities` (`List<Map<String, String>>`):**
   - Danh sách các thành phố có rạp đang hoạt động: trích xuất `DISTINCT city` từ bảng `cinema.cinemas` (`status = 'ACTIVE'`).

---

## 3. Kiến Trúc Điều Phối & Tái Sử Dụng Dịch Vụ (Aggregator Pattern)

`HomeService` đóng vai trò là một **Aggregator / Orchestrator** thuần túy, gọi trực tiếp các dịch vụ từ module `movie` và `cinema` mà **không tạo DAO trùng lặp**:

- **Module `movie` (`MovieService`):**
  - Cung cấp `movieService.getMovies(MovieRequest)` để lấy `nowShowing` (`status = 'ACTIVE'`) và `comingSoon` (`status = 'COMING_SOON'`).
  - Cung cấp `movieService.getGenres()` để lấy toàn bộ thể loại phim (`Genre`).
- **Module `cinema` (`CinemaService`):**
  - Cung cấp `cinemaService.getCinemas(CinemaRequest)` để lấy `featuredCinemas` (`status = 'ACTIVE'`).
  - Cung cấp `cinemaService.getCities()` để lấy danh mục các thành phố (`CityItem`).
