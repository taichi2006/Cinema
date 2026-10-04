package com.cinema.movie;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.cinema.common.dto.CommonDTO.PageMeta;
import com.cinema.common.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MovieServiceTest {

    private MovieService movieService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Stub DAO for testing validation & service layer logic without database dependency
    private static class StubMovieDAO extends MovieDAO {
        private List<Movie> stubMovies = Collections.emptyList();
        private long stubCount = 0L;

        public void setStubData(List<Movie> movies, long count) {
            this.stubMovies = movies;
            this.stubCount = count;
        }

        @Override
        public Optional<Movie> findById(Long movieId) {
            for (Movie movie : stubMovies) {
                if (movie.getMovieId() != null && movie.getMovieId().equals(movieId)) {
                    return Optional.of(movie);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<Movie> findMovies(MovieRequest request) {
            return stubMovies;
        }

        @Override
        public long countMovies(MovieRequest request) {
            return stubCount;
        }

        private List<ShowtimeResponse> stubShowtimes = Collections.emptyList();
        private long stubShowtimeCount = 0L;

        public void setStubShowtimeData(List<ShowtimeResponse> showtimes, long count) {
            this.stubShowtimes = showtimes;
            this.stubShowtimeCount = count;
        }

        @Override
        public List<ShowtimeResponse> findShowtimes(Long movieId, LocalDate date, Long cinemaId, int page, int size) {
            return stubShowtimes;
        }

        @Override
        public long countShowtimes(Long movieId, LocalDate date, Long cinemaId) {
            return stubShowtimeCount;
        }
    }

    private StubMovieDAO stubDAO;

    @BeforeEach
    void setUp() {
        stubDAO = new StubMovieDAO();
        movieService = new MovieService(stubDAO);
    }

    @Test
    @DisplayName("Ném ApiException 400 khi q vượt quá 100 ký tự")
    void testQueryTooLong() {
        String longQuery = "a".repeat(101);
        MovieRequest request = new MovieRequest(longQuery, null, null, 0, 20, "releaseDate,desc");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> movieService.getMovies(request)
        );
        assertEquals(400, exception.getStatus());
        assertEquals("Từ khóa tìm kiếm 'q' không được vượt quá 100 ký tự.", exception.getMessage());
    }

    @Test
    @DisplayName("Ném ApiException 400 khi status không hợp lệ")
    void testInvalidStatus() {
        MovieRequest request = new MovieRequest(null, null, "INVALID_STATUS", 0, 20, "releaseDate,desc");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> movieService.getMovies(request)
        );
        assertEquals(400, exception.getStatus());
        assertEquals(
                "Trạng thái 'status' không hợp lệ: INVALID_STATUS. Các giá trị hợp lệ: [NOW_SHOWING, COMING_SOON, ENDED]",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Ném ApiException 400 khi sort không hợp lệ")
    void testInvalidSort() {
        MovieRequest request = new MovieRequest(null, null, null, 0, 20, "unknown_field,asc");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> movieService.getMovies(request)
        );
        assertEquals(400, exception.getStatus());
        assertEquals(
                "Tham số 'sort' không hợp lệ: unknown_field,asc. Các giá trị hợp lệ: [releaseDate,asc, releaseDate,desc, title,asc, title,desc]",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("Ném ApiException 400 khi page < 0")
    void testInvalidPage() {
        MovieRequest request = new MovieRequest(null, null, null, -1, 20, "releaseDate,desc");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> movieService.getMovies(request)
        );
        assertEquals(400, exception.getStatus());
        assertEquals("Số trang 'page' phải lớn hơn hoặc bằng 0.", exception.getMessage());
    }

    @Test
    @DisplayName("Ném ApiException 400 khi size < 1 hoặc > 50")
    void testInvalidSize() {
        MovieRequest requestNegativeSize = new MovieRequest(null, null, null, 0, 0, "releaseDate,desc");
        ApiException exNeg = assertThrows(
                ApiException.class,
                () -> movieService.getMovies(requestNegativeSize)
        );
        assertEquals(400, exNeg.getStatus());

        MovieRequest requestTooLargeSize = new MovieRequest(null, null, null, 0, 51, "releaseDate,desc");
        ApiException exLarge = assertThrows(
                ApiException.class,
                () -> movieService.getMovies(requestTooLargeSize)
        );
        assertEquals(400, exLarge.getStatus());
    }

    @Test
    @DisplayName("Ném ApiException 400 khi page=2147483647&size=50 gây tràn số offset")
    void testPageMaxIntOverflow() {
        MovieRequest request = new MovieRequest(null, null, null, 2147483647, 50, "releaseDate,desc");

        ApiException exception = assertThrows(
                ApiException.class,
                () -> movieService.getMovies(request)
        );
        assertEquals(400, exception.getStatus());
        assertEquals("Vị trí phân trang vượt quá giới hạn cho phép.", exception.getMessage());
    }

    @Test
    @DisplayName("JSON hoàn chỉnh khớp 100% schema: success, data với id string, averageRating, reviewCount, meta (không có traceId)")
    void testJsonMatchesExactSchema() throws Exception {
        Movie movie = new Movie();
        movie.setMovieId(1L);
        movie.setTitle("Mai");
        movie.setPosterUrl("https://example.com/poster.jpg");
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.of(2026, 10, 3));
        movie.setStatus("NOW_SHOWING");
        movie.setAgeRating("T18");

        stubDAO.setStubData(List.of(movie), 1L);

        MovieRequest request = new MovieRequest(null, null, null, 0, 20, null);
        Map<String, Object> response = movieService.getMovies(request);

        String json = objectMapper.writeValueAsString(response);
        JsonNode root = objectMapper.readTree(json);

        // Kiểm tra root
        assertTrue(root.has("success"), "Phải có thuộc tính success");
        assertTrue(root.get("success").asBoolean(), "success phải là true");
        assertFalse(root.has("traceId"), "Không được có thuộc tính traceId");

        // Kiểm tra meta ở root level (cục PageMeta ở dưới)
        JsonNode meta = root.get("meta");
        assertNotNull(meta, "root phải chứa meta");
        assertEquals(0, meta.get("page").asInt());
        assertEquals(20, meta.get("size").asInt());
        assertEquals(1, meta.get("totalElements").asLong());
        assertEquals(1, meta.get("totalPages").asInt());

        // Kiểm tra data ở root level (cục Success ở trên - data là array trực tiếp)
        JsonNode firstMovie = root.get("data").get(0);
        assertEquals("1", firstMovie.get("id").asText(), "id phải là kiểu chuỗi String");
        assertEquals("Mai", firstMovie.get("title").asText());
        assertEquals("https://example.com/poster.jpg", firstMovie.get("posterUrl").asText());
        assertEquals(120, firstMovie.get("durationMinutes").asInt());
        assertEquals("2026-10-03", firstMovie.get("releaseDate").asText());
        assertEquals("NOW_SHOWING", firstMovie.get("status").asText());
        assertEquals("T18", firstMovie.get("ageRating").asText());
        assertEquals(0.0, firstMovie.get("averageRating").asDouble());
        assertEquals(0, firstMovie.get("reviewCount").asInt());

        assertFalse(firstMovie.has("trailerUrl"), "Không được có thuộc tính trailerUrl");
        assertFalse(firstMovie.has("rawReleaseDate"), "Không được có thuộc tính rawReleaseDate");
    }

    @Test
    @DisplayName("Lọc phim có nhiều thể loại: phim không trùng và trả đủ thể loại không trùng lặp")
    void testMovieWithMultipleGenres() {
        Movie movie = new Movie();
        movie.setMovieId(1L);
        movie.setTitle("Dune: Part Two");
        movie.setDurationMinutes(166);
        movie.setReleaseDate(LocalDate.of(2024, 3, 1));
        movie.setStatus("NOW_SHOWING");

        Genre genre1 = new Genre(1L, "SCI_FI", "Khoa học viễn tưởng");
        Genre genre2 = new Genre(2L, "ACTION", "Hành động");
        Genre duplicateGenre = new Genre(1L, "SCI_FI", "Khoa học viễn tưởng");

        movie.setGenres(List.of(genre1, genre2, duplicateGenre));

        stubDAO.setStubData(List.of(movie), 1L);

        MovieRequest request = new MovieRequest(null, "Khoa học viễn tưởng", null, 0, 20, null);
        Map<String, Object> response = movieService.getMovies(request);

        @SuppressWarnings("unchecked")
        List<MovieResponse> data = (List<MovieResponse>) response.get("data");
        assertEquals(1, data.size());
        MovieResponse movieResponse = data.get(0);
        assertEquals(2, movieResponse.getGenres().size(), "Các thể loại trùng lặp phải được lọc sạch");
        assertEquals(List.of("Khoa học viễn tưởng", "Hành động"), movieResponse.getGenres());
    }

    @Test
    @DisplayName("Tìm không có kết quả: 200, data: [], tổng số phần tử bằng 0")
    void testNoResultsFound() {
        stubDAO.setStubData(Collections.emptyList(), 0L);

        MovieRequest request = new MovieRequest("KhongTonTai123456", null, null, 0, 20, null);
        Map<String, Object> response = assertDoesNotThrow(
                () -> movieService.getMovies(request)
        );

        assertNotNull(response);
        assertNotNull(response.get("data"));
        @SuppressWarnings("unchecked")
        List<MovieResponse> data = (List<MovieResponse>) response.get("data");
        assertTrue(data.isEmpty(), "data phải là mảng rỗng []");
        PageMeta meta = (PageMeta) response.get("meta");
        assertNotNull(meta);
        assertEquals(0L, meta.getTotalElements(), "tổng số phần tử bằng 0");
        assertEquals(0, meta.getTotalPages());
    }

    @Test
    @DisplayName("ApiException.badRequest tạo ngoại lệ với HTTP status 400 và message chuẩn")
    void testApiExceptionBadRequest() {
        ApiException exception = ApiException.badRequest("Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50.");
        assertEquals(400, exception.getStatus());
        assertEquals("Kích thước trang 'size' phải nằm trong khoảng từ 1 đến 50.", exception.getMessage());
    }

    @Test
    @DisplayName("Tìm chi tiết phim thành công: HTTP 200, đúng MovieDetail schema, JSON không chứa meta")
    void testFindById_Success() throws Exception {
        Movie movie = new Movie();
        movie.setMovieId(1L);
        movie.setTitle("Mai");
        movie.setDescription("Phim tâm lý tình cảm Việt Nam");
        movie.setDurationMinutes(120);
        movie.setReleaseDate(LocalDate.of(2024, 2, 10));
        movie.setPosterUrl("https://example.com/poster.jpg");
        movie.setTrailerUrl("https://example.com/trailer.mp4");
        movie.setLanguage("VI");
        movie.setDefaultFormat("2D");
        movie.setAgeRating("T18");
        movie.setAgeLimit(18);
        movie.setStatus("NOW_SHOWING");

        Genre genre1 = new Genre(1L, "DRAMA", "Tâm lý");
        Genre genre2 = new Genre(2L, "ROMANCE", "Lãng mạn");
        movie.setGenres(List.of(genre1, genre2));

        stubDAO.setStubData(List.of(movie), 1L);

        ApiResponse<MovieResponse> response = movieService.getMovieById("1");

        assertNotNull(response);
        assertTrue(response.isSuccess());

        MovieResponse detail = response.getData();
        assertNotNull(detail);
        assertEquals("1", detail.getId());
        assertEquals("Mai", detail.getTitle());
        assertEquals("Phim tâm lý tình cảm Việt Nam", detail.getDescription());
        assertEquals(120, detail.getDurationMinutes());
        assertEquals("2024-02-10", detail.getReleaseDate());
        assertEquals("T18", detail.getAgeRating());
        assertEquals(18, detail.getAgeLimit());
        assertEquals(List.of("Tâm lý", "Lãng mạn"), detail.getGenres());
        assertEquals(0.0, detail.getAverageRating());
        assertEquals(0, detail.getReviewCount());

        // Kiểm tra JSON serialize: ẩn meta khi null
        String json = objectMapper.writeValueAsString(response);
        JsonNode root = objectMapper.readTree(json);

        assertTrue(root.has("success"));
        assertTrue(root.get("success").asBoolean());
        assertTrue(root.has("data"));
        assertFalse(root.has("meta"), "JSON không được chứa trường meta");
        assertEquals("1", root.get("data").get("id").asText());
    }

    @Test
    @DisplayName("Tìm chi tiết phim không tồn tại: ném ApiException 404")
    void testFindById_NotFound() {
        stubDAO.setStubData(Collections.emptyList(), 0L);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> movieService.getMovieById("99999")
        );

        assertEquals(404, ex.getStatus());
        assertEquals("Không tìm thấy phim", ex.getMessage());
    }

    @Test
    @DisplayName("Tìm chi tiết phim với ID không hợp lệ: ném ApiException 400")
    void testFindById_InvalidId() {
        ApiException exString = assertThrows(
                ApiException.class,
                () -> movieService.getMovieById("abc")
        );
        assertEquals(400, exString.getStatus());

        ApiException exNegative = assertThrows(
                ApiException.class,
                () -> movieService.getMovieById("-5")
        );
        assertEquals(400, exNegative.getStatus());

        ApiException exBlank = assertThrows(
                ApiException.class,
                () -> movieService.getMovieById("   ")
        );
        assertEquals(400, exBlank.getStatus());
    }

    @Test
    @DisplayName("Lấy danh sách suất chiếu thành công: trả 200, success, data showtimes và meta")
    void testGetMovieShowtimes_Success() throws Exception {
        Movie movie = new Movie();
        movie.setMovieId(1L);
        stubDAO.setStubData(List.of(movie), 1L);

        ShowtimeResponse st = new ShowtimeResponse(
                "101", "1", "1", "Galaxy Nguyễn Du",
                "5", "Cinema 1", "2026-10-05T14:30:00Z", "2026-10-05T16:30:00Z",
                "2D", "VI", 95000L, "OPEN"
        );
        stubDAO.setStubShowtimeData(List.of(st), 1L);

        Map<String, Object> result = movieService.getMovieShowtimes("1", "2026-10-05", "1", 0, 20);

        assertNotNull(result);
        assertEquals(true, result.get("success"));

        String json = objectMapper.writeValueAsString(result);
        JsonNode root = objectMapper.readTree(json);

        assertTrue(root.has("success"));
        assertTrue(root.get("success").asBoolean());
        assertTrue(root.has("data"));
        assertTrue(root.has("meta"));

        JsonNode data = root.get("data");
        assertEquals(1, data.size());
        assertEquals("101", data.get(0).get("id").asText());
        assertEquals("Galaxy Nguyễn Du", data.get(0).get("cinemaName").asText());
        assertEquals("OPEN", data.get(0).get("status").asText());

        JsonNode meta = root.get("meta");
        assertEquals(0, meta.get("page").asInt());
        assertEquals(20, meta.get("size").asInt());
        assertEquals(1, meta.get("totalElements").asLong());
        assertEquals(1, meta.get("totalPages").asInt());
    }

    @Test
    @DisplayName("Lấy suất chiếu thiếu tham số date: ném ApiException 400")
    void testGetMovieShowtimes_MissingDate() {
        Movie movie = new Movie();
        movie.setMovieId(1L);
        stubDAO.setStubData(List.of(movie), 1L);

        ApiException exNull = assertThrows(
                ApiException.class,
                () -> movieService.getMovieShowtimes("1", null, null, 0, 20)
        );
        assertEquals(400, exNull.getStatus());
        assertTrue(exNull.getMessage().contains("date"));

        ApiException exBlank = assertThrows(
                ApiException.class,
                () -> movieService.getMovieShowtimes("1", "   ", null, 0, 20)
        );
        assertEquals(400, exBlank.getStatus());
    }

    @Test
    @DisplayName("Lấy suất chiếu sai định dạng date: ném ApiException 400")
    void testGetMovieShowtimes_InvalidDateFormat() {
        Movie movie = new Movie();
        movie.setMovieId(1L);
        stubDAO.setStubData(List.of(movie), 1L);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> movieService.getMovieShowtimes("1", "2026/10/05", null, 0, 20)
        );
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("YYYY-MM-DD"));
    }

    @Test
    @DisplayName("Lấy suất chiếu phim không tồn tại: ném ApiException 404")
    void testGetMovieShowtimes_MovieNotFound() {
        stubDAO.setStubData(Collections.emptyList(), 0L);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> movieService.getMovieShowtimes("9999", "2026-10-05", null, 0, 20)
        );
        assertEquals(404, ex.getStatus());
        assertEquals("Không tìm thấy phim", ex.getMessage());
    }

    @Test
    @DisplayName("Lấy suất chiếu cinemaId không hợp lệ: ném ApiException 400")
    void testGetMovieShowtimes_InvalidCinemaId() {
        Movie movie = new Movie();
        movie.setMovieId(1L);
        stubDAO.setStubData(List.of(movie), 1L);

        ApiException exStr = assertThrows(
                ApiException.class,
                () -> movieService.getMovieShowtimes("1", "2026-10-05", "abc", 0, 20)
        );
        assertEquals(400, exStr.getStatus());

        ApiException exNeg = assertThrows(
                ApiException.class,
                () -> movieService.getMovieShowtimes("1", "2026-10-05", "-1", 0, 20)
        );
        assertEquals(400, exNeg.getStatus());
    }
}
