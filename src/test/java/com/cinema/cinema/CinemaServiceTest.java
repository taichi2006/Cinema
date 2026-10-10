package com.cinema.cinema;

import com.cinema.cinema.DTO.Request.CinemaRequest;
import com.cinema.cinema.DTO.Response.CinemaResponse;
import com.cinema.cinema.DTO.Response.CinemaShowtimeResponse;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CinemaServiceTest {

    private static class StubCinemaDAO extends CinemaDAO {
        private List<Cinema> stubCinemas = Collections.emptyList();
        private long stubCount = 0L;
        private List<CinemaShowtimeResponse> stubShowtimes = Collections.emptyList();
        private long stubShowtimeCount = 0L;

        public void setStubData(List<Cinema> cinemas, long count) {
            this.stubCinemas = cinemas;
            this.stubCount = count;
        }

        public void setStubShowtimeData(List<CinemaShowtimeResponse> showtimes, long count) {
            this.stubShowtimes = showtimes;
            this.stubShowtimeCount = count;
        }

        @Override
        public Optional<Cinema> findById(Long cinemaId) {
            for (Cinema cinema : stubCinemas) {
                if (cinema.getCinemaId() != null && cinema.getCinemaId().equals(cinemaId)) {
                    return Optional.of(cinema);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<Cinema> findCinemas(CinemaRequest filter) {
            return stubCinemas;
        }

        @Override
        public long countCinemas(CinemaRequest filter) {
            return stubCount;
        }

        @Override
        public List<CinemaShowtimeResponse> findShowtimes(Long cinemaId, LocalDate date, Long movieId, int page, int size) {
            return stubShowtimes;
        }

        @Override
        public long countShowtimes(Long cinemaId, LocalDate date, Long movieId) {
            return stubShowtimeCount;
        }
    }

    private StubCinemaDAO stubDAO;
    private CinemaService cinemaService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        stubDAO = new StubCinemaDAO();
        cinemaService = new CinemaService(stubDAO);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Lấy danh sách rạp thành công: trả về đầy đủ các trường và meta")
    void testGetCinemas_Success() throws Exception {
        Cinema cinema = new Cinema(
                1L,
                "Galaxy Nguyễn Du",
                "116 Nguyễn Du, Quận 1, TP.HCM",
                "HCM",
                "Hồ Chí Minh",
                "028 3823 4567",
                "https://example.com/cinema1.jpg",
                10.7725,
                106.698,
                "ACTIVE"
        );
        stubDAO.setStubData(List.of(cinema), 1L);

        CinemaRequest request = new CinemaRequest(null, null, 0, 20, "name,asc");
        Map<String, Object> result = cinemaService.getCinemas(request);

        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertNotNull(result.get("data"));
        assertNotNull(result.get("meta"));

        @SuppressWarnings("unchecked")
        List<CinemaResponse> data = (List<CinemaResponse>) result.get("data");
        assertEquals(1, data.size());

        CinemaResponse item = data.get(0);
        assertEquals("1", item.getId());
        assertEquals("Galaxy Nguyễn Du", item.getName());
        assertEquals("HCM", item.getCityCode());
        assertEquals("Hồ Chí Minh", item.getCityName());
        assertEquals("116 Nguyễn Du, Quận 1, TP.HCM", item.getAddress());
        assertEquals("028 3823 4567", item.getPhone());
        assertEquals("https://example.com/cinema1.jpg", item.getImageUrl());
        assertEquals(10.7725, item.getLatitude());
        assertEquals(106.698, item.getLongitude());

        PageMeta meta = (PageMeta) result.get("meta");
        assertEquals(0, meta.getPage());
        assertEquals(20, meta.getSize());
        assertEquals(1L, meta.getTotalElements());
        assertEquals(1, meta.getTotalPages());

        // Kiểm tra serialization JSON
        String json = objectMapper.writeValueAsString(result);
        JsonNode root = objectMapper.readTree(json);
        assertTrue(root.has("success"));
        assertTrue(root.get("success").asBoolean());
        assertTrue(root.has("data"));
        assertTrue(root.has("meta"));
        assertEquals("Galaxy Nguyễn Du", root.get("data").get(0).get("name").asText());
    }

    @Test
    @DisplayName("Sort mặc định: khi không truyền sort, tự động gán name,asc")
    void testGetCinemas_DefaultSort() {
        stubDAO.setStubData(Collections.emptyList(), 0L);

        CinemaRequest request = new CinemaRequest(null, null, 0, 20, null);
        assertDoesNotThrow(() -> cinemaService.getCinemas(request));
        assertEquals("name,asc", request.getSort());
    }

    @Test
    @DisplayName("Sort name,desc: hợp lệ và thực hiện thành công")
    void testGetCinemas_SortDesc() {
        stubDAO.setStubData(Collections.emptyList(), 0L);

        CinemaRequest request = new CinemaRequest(null, null, 0, 20, "name,desc");
        Map<String, Object> result = cinemaService.getCinemas(request);
        assertEquals(true, result.get("success"));
    }

    @Test
    @DisplayName("Sort không hợp lệ: ném ApiException 400")
    void testGetCinemas_InvalidSort() {
        CinemaRequest request = new CinemaRequest(null, null, 0, 20, "invalid_sort");

        ApiException ex = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemas(request)
        );
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("sort"));
    }

    @Test
    @DisplayName("Từ khóa q vượt quá 100 ký tự: ném ApiException 400")
    void testGetCinemas_QTooLong() {
        String longQ = "a".repeat(101);
        CinemaRequest request = new CinemaRequest(null, longQ, 0, 20, "name,asc");

        ApiException ex = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemas(request)
        );
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("100"));
    }

    @Test
    @DisplayName("Page âm: ném ApiException 400")
    void testGetCinemas_NegativePage() {
        CinemaRequest request = new CinemaRequest(null, null, -1, 20, "name,asc");

        ApiException ex = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemas(request)
        );
        assertEquals(400, ex.getStatus());
        assertTrue(ex.getMessage().contains("page"));
    }

    @Test
    @DisplayName("Size không nằm trong [1, 50]: ném ApiException 400")
    void testGetCinemas_InvalidSize() {
        CinemaRequest reqZero = new CinemaRequest(null, null, 0, 0, "name,asc");
        ApiException exZero = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemas(reqZero)
        );
        assertEquals(400, exZero.getStatus());

        CinemaRequest reqTooLarge = new CinemaRequest(null, null, 0, 51, "name,asc");
        ApiException exTooLarge = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemas(reqTooLarge)
        );
        assertEquals(400, exTooLarge.getStatus());
    }

    @Test
    @DisplayName("Phân trang tràn số: ném ApiException 400")
    void testGetCinemas_OffsetOverflow() {
        CinemaRequest request = new CinemaRequest(null, null, Integer.MAX_VALUE, 50, "name,asc");

        ApiException ex = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemas(request)
        );
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("Danh sách rạp rỗng: trả về data rỗng, totalElements 0, totalPages 0")
    void testGetCinemas_EmptyResults() {
        stubDAO.setStubData(Collections.emptyList(), 0L);

        CinemaRequest request = new CinemaRequest("HCM", "Không tồn tại", 0, 20, "name,asc");
        Map<String, Object> result = cinemaService.getCinemas(request);

        assertEquals(true, result.get("success"));
        @SuppressWarnings("unchecked")
        List<CinemaResponse> data = (List<CinemaResponse>) result.get("data");
        assertTrue(data.isEmpty());

        PageMeta meta = (PageMeta) result.get("meta");
        assertEquals(0L, meta.getTotalElements());
        assertEquals(0, meta.getTotalPages());
    }

    @Test
    @DisplayName("Lấy chi tiết rạp thành công: trả về đầy đủ các trường và meta rỗng")
    void testGetCinemaById_Success() throws Exception {
        Cinema cinema = new Cinema(
                1L,
                "Galaxy Nguyễn Du",
                "116 Nguyễn Du, Quận 1, TP.HCM",
                "HCM",
                "Hồ Chí Minh",
                "028 3823 4567",
                "https://example.com/cinema1.jpg",
                10.7725,
                106.698,
                "ACTIVE"
        );
        stubDAO.setStubData(List.of(cinema), 1L);

        Map<String, Object> result = cinemaService.getCinemaById("1");

        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertNotNull(result.get("data"));
        assertNotNull(result.get("meta"));

        CinemaResponse item = (CinemaResponse) result.get("data");
        assertEquals("1", item.getId());
        assertEquals("Galaxy Nguyễn Du", item.getName());
        assertEquals("HCM", item.getCityCode());
        assertEquals("Hồ Chí Minh", item.getCityName());
        assertEquals("116 Nguyễn Du, Quận 1, TP.HCM", item.getAddress());
        assertEquals("028 3823 4567", item.getPhone());
        assertEquals("https://example.com/cinema1.jpg", item.getImageUrl());
        assertEquals(10.7725, item.getLatitude());
        assertEquals(106.698, item.getLongitude());

        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) result.get("meta");
        assertTrue(meta.isEmpty());

        // Kiểm tra JSON serialization
        String json = objectMapper.writeValueAsString(result);
        JsonNode root = objectMapper.readTree(json);
        assertTrue(root.get("success").asBoolean());
        assertEquals("1", root.get("data").get("id").asText());
        assertEquals("Galaxy Nguyễn Du", root.get("data").get("name").asText());
        assertTrue(root.get("meta").isObject());
        assertEquals(0, root.get("meta").size());
    }

    @Test
    @DisplayName("Lấy chi tiết rạp không tồn tại: ném ApiException 404")
    void testGetCinemaById_NotFound() {
        stubDAO.setStubData(Collections.emptyList(), 0L);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaById("9999")
        );
        assertEquals(404, ex.getStatus());
        assertEquals("Không tìm thấy rạp", ex.getMessage());
    }

    @Test
    @DisplayName("Lấy chi tiết rạp đang ở trạng thái INACTIVE: ném ApiException 404")
    void testGetCinemaById_InactiveCinema() {
        Cinema inactiveCinema = new Cinema(
                2L,
                "Galaxy Tân Bình (Đóng cửa)",
                "Hoàng Hoa Thám, Tân Bình",
                "HCM",
                "Hồ Chí Minh",
                null,
                null,
                null,
                null,
                "INACTIVE"
        );
        stubDAO.setStubData(List.of(inactiveCinema), 1L);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaById("2")
        );
        assertEquals(404, ex.getStatus());
        assertEquals("Không tìm thấy rạp", ex.getMessage());
    }

    @Test
    @DisplayName("Mã rạp id không hợp lệ (null, trống, âm, chữ): ném ApiException 400")
    void testGetCinemaById_InvalidId() {
        ApiException exNull = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaById(null)
        );
        assertEquals(400, exNull.getStatus());

        ApiException exEmpty = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaById("   ")
        );
        assertEquals(400, exEmpty.getStatus());

        ApiException exNegative = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaById("-1")
        );
        assertEquals(400, exNegative.getStatus());

        ApiException exZero = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaById("0")
        );
        assertEquals(400, exZero.getStatus());

        ApiException exStr = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaById("abc")
        );
        assertEquals(400, exStr.getStatus());
    }

    @Test
    @DisplayName("Lấy lịch chiếu của rạp thành công: trả về đủ 14 trường và phân trang")
    void testGetCinemaShowtimes_Success() throws Exception {
        Cinema cinema = new Cinema(
                1L,
                "Galaxy Nguyễn Du",
                "116 Nguyễn Du, Quận 1, TP.HCM",
                "HCM",
                "Hồ Chí Minh",
                "028 3823 4567",
                "https://example.com/c1.jpg",
                10.7725,
                106.698,
                "ACTIVE"
        );
        stubDAO.setStubData(List.of(cinema), 1L);

        CinemaShowtimeResponse showtime = new CinemaShowtimeResponse(
                "101",
                "10",
                "Dune: Part Two",
                "1",
                "Galaxy Nguyễn Du",
                "5",
                "Cinema 1",
                "2026-10-05T18:00:00+07:00",
                "2026-10-05T20:30:00+07:00",
                "2D",
                "VietSub",
                85000L,
                "VND",
                95
        );
        stubDAO.setStubShowtimeData(List.of(showtime), 1L);

        Map<String, Object> result = cinemaService.getCinemaShowtimes("1", "2026-10-05", null, 0, 20);

        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertNotNull(result.get("data"));
        assertNotNull(result.get("meta"));

        @SuppressWarnings("unchecked")
        List<CinemaShowtimeResponse> data = (List<CinemaShowtimeResponse>) result.get("data");
        assertEquals(1, data.size());

        CinemaShowtimeResponse item = data.get(0);
        assertEquals("101", item.getId());
        assertEquals("10", item.getMovieId());
        assertEquals("Dune: Part Two", item.getMovieTitle());
        assertEquals("1", item.getCinemaId());
        assertEquals("Galaxy Nguyễn Du", item.getCinemaName());
        assertEquals("5", item.getRoomId());
        assertEquals("Cinema 1", item.getRoomName());
        assertEquals("2026-10-05T18:00:00+07:00", item.getStartsAt());
        assertEquals("2026-10-05T20:30:00+07:00", item.getEndsAt());
        assertEquals("2D", item.getFormat());
        assertEquals("VietSub", item.getLanguage());
        assertEquals(85000L, item.getMinTicketPrice());
        assertEquals("VND", item.getCurrency());
        assertEquals(95, item.getAvailableSeatCount());

        PageMeta meta = (PageMeta) result.get("meta");
        assertEquals(0, meta.getPage());
        assertEquals(20, meta.getSize());
        assertEquals(1L, meta.getTotalElements());
        assertEquals(1, meta.getTotalPages());

        // Kiểm tra serialization JSON
        String json = objectMapper.writeValueAsString(result);
        JsonNode root = objectMapper.readTree(json);
        assertTrue(root.get("success").asBoolean());
        JsonNode itemNode = root.get("data").get(0);
        assertEquals("101", itemNode.get("id").asText());
        assertEquals("10", itemNode.get("movieId").asText());
        assertEquals("Dune: Part Two", itemNode.get("movieTitle").asText());
        assertEquals("1", itemNode.get("cinemaId").asText());
        assertEquals("Galaxy Nguyễn Du", itemNode.get("cinemaName").asText());
        assertEquals("5", itemNode.get("roomId").asText());
        assertEquals("Cinema 1", itemNode.get("roomName").asText());
        assertEquals("2026-10-05T18:00:00+07:00", itemNode.get("startsAt").asText());
        assertEquals("2026-10-05T20:30:00+07:00", itemNode.get("endsAt").asText());
        assertEquals("2D", itemNode.get("format").asText());
        assertEquals("VietSub", itemNode.get("language").asText());
        assertEquals(85000L, itemNode.get("minTicketPrice").asLong());
        assertEquals("VND", itemNode.get("currency").asText());
        assertEquals(95, itemNode.get("availableSeatCount").asInt());
    }

    @Test
    @DisplayName("Lọc suất chiếu rạp theo movieId thành công")
    void testGetCinemaShowtimes_FilterByMovieId() {
        Cinema cinema = new Cinema(
                1L,
                "Galaxy Nguyễn Du",
                "116 Nguyễn Du, Quận 1, TP.HCM",
                "HCM",
                "Hồ Chí Minh",
                null,
                null,
                null,
                null,
                "ACTIVE"
        );
        stubDAO.setStubData(List.of(cinema), 1L);

        CinemaShowtimeResponse showtime = new CinemaShowtimeResponse(
                "102", "10", "Dune: Part Two", "1", "Galaxy Nguyễn Du", "5", "Cinema 1",
                "2026-10-05T21:00:00+07:00", "2026-10-05T23:30:00+07:00",
                "IMAX", "VietSub", 120000L, "VND", 50
        );
        stubDAO.setStubShowtimeData(List.of(showtime), 1L);

        Map<String, Object> result = cinemaService.getCinemaShowtimes("1", "2026-10-05", "10", 0, 10);
        assertNotNull(result);
        assertEquals(true, result.get("success"));

        @SuppressWarnings("unchecked")
        List<CinemaShowtimeResponse> data = (List<CinemaShowtimeResponse>) result.get("data");
        assertEquals(1, data.size());
        assertEquals("102", data.get(0).getId());
        assertEquals("10", data.get(0).getMovieId());
    }

    @Test
    @DisplayName("Lấy suất chiếu: Rạp không tồn tại -> ném 404")
    void testGetCinemaShowtimes_CinemaNotFound() {
        stubDAO.setStubData(Collections.emptyList(), 0L);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("999", "2026-10-05", null, 0, 20)
        );
        assertEquals(404, ex.getStatus());
        assertEquals("Không tìm thấy rạp", ex.getMessage());
    }

    @Test
    @DisplayName("Lấy suất chiếu: Rạp INACTIVE -> ném 404")
    void testGetCinemaShowtimes_InactiveCinema() {
        Cinema inactiveCinema = new Cinema(
                2L, "Galaxy Cũ", "Địa chỉ", "HCM", "Hồ Chí Minh",
                null, null, null, null, "INACTIVE"
        );
        stubDAO.setStubData(List.of(inactiveCinema), 1L);

        ApiException ex = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("2", "2026-10-05", null, 0, 20)
        );
        assertEquals(404, ex.getStatus());
        assertEquals("Không tìm thấy rạp", ex.getMessage());
    }

    @Test
    @DisplayName("Lấy suất chiếu: Thiếu tham số bắt buộc date (null hoặc khoảng trắng) -> ném 400")
    void testGetCinemaShowtimes_MissingDate() {
        Cinema cinema = new Cinema(1L, "Galaxy Nguyễn Du", null, "HCM", "HCM", null, null, null, null, "ACTIVE");
        stubDAO.setStubData(List.of(cinema), 1L);

        ApiException exNull = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", null, null, 0, 20)
        );
        assertEquals(400, exNull.getStatus());
        assertTrue(exNull.getMessage().contains("bắt buộc"));

        ApiException exBlank = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", "   ", null, 0, 20)
        );
        assertEquals(400, exBlank.getStatus());
    }

    @Test
    @DisplayName("Lấy suất chiếu: Sai định dạng date -> ném 400")
    void testGetCinemaShowtimes_InvalidDateFormat() {
        Cinema cinema = new Cinema(1L, "Galaxy Nguyễn Du", null, "HCM", "HCM", null, null, null, null, "ACTIVE");
        stubDAO.setStubData(List.of(cinema), 1L);

        ApiException ex1 = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", "invalid-date", null, 0, 20)
        );
        assertEquals(400, ex1.getStatus());

        ApiException ex2 = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", "05-10-2026", null, 0, 20)
        );
        assertEquals(400, ex2.getStatus());

        ApiException ex3 = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", "2026/10/05", null, 0, 20)
        );
        assertEquals(400, ex3.getStatus());
    }

    @Test
    @DisplayName("Lấy suất chiếu: movieId không hợp lệ (âm, 0, chữ) -> ném 400")
    void testGetCinemaShowtimes_InvalidMovieId() {
        Cinema cinema = new Cinema(1L, "Galaxy Nguyễn Du", null, "HCM", "HCM", null, null, null, null, "ACTIVE");
        stubDAO.setStubData(List.of(cinema), 1L);

        ApiException exAlpha = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", "2026-10-05", "abc", 0, 20)
        );
        assertEquals(400, exAlpha.getStatus());

        ApiException exNegative = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", "2026-10-05", "-5", 0, 20)
        );
        assertEquals(400, exNegative.getStatus());

        ApiException exZero = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", "2026-10-05", "0", 0, 20)
        );
        assertEquals(400, exZero.getStatus());
    }

    @Test
    @DisplayName("Lấy suất chiếu: Phân trang page hoặc size không hợp lệ -> ném 400")
    void testGetCinemaShowtimes_InvalidPageAndSize() {
        Cinema cinema = new Cinema(1L, "Galaxy Nguyễn Du", null, "HCM", "HCM", null, null, null, null, "ACTIVE");
        stubDAO.setStubData(List.of(cinema), 1L);

        // page < 0
        ApiException exPage = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", "2026-10-05", null, -1, 20)
        );
        assertEquals(400, exPage.getStatus());

        // size < 1
        ApiException exSizeZero = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", "2026-10-05", null, 0, 0)
        );
        assertEquals(400, exSizeZero.getStatus());

        // size > 50
        ApiException exSizeOver = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", "2026-10-05", null, 0, 51)
        );
        assertEquals(400, exSizeOver.getStatus());

        // Tràn số phân trang
        ApiException exOverflow = assertThrows(
                ApiException.class,
                () -> cinemaService.getCinemaShowtimes("1", "2026-10-05", null, Integer.MAX_VALUE, 10)
        );
        assertEquals(400, exOverflow.getStatus());
    }

    @Test
    @DisplayName("Lấy suất chiếu: Danh sách rỗng khi không có suất chiếu thỏa mãn")
    void testGetCinemaShowtimes_EmptyResults() {
        Cinema cinema = new Cinema(1L, "Galaxy Nguyễn Du", null, "HCM", "HCM", null, null, null, null, "ACTIVE");
        stubDAO.setStubData(List.of(cinema), 1L);
        stubDAO.setStubShowtimeData(Collections.emptyList(), 0L);

        Map<String, Object> result = cinemaService.getCinemaShowtimes("1", "2026-10-05", null, 0, 20);

        assertNotNull(result);
        assertEquals(true, result.get("success"));

        @SuppressWarnings("unchecked")
        List<CinemaShowtimeResponse> data = (List<CinemaShowtimeResponse>) result.get("data");
        assertTrue(data.isEmpty());

        PageMeta meta = (PageMeta) result.get("meta");
        assertEquals(0, meta.getPage());
        assertEquals(20, meta.getSize());
        assertEquals(0L, meta.getTotalElements());
        assertEquals(0, meta.getTotalPages());
    }
}
