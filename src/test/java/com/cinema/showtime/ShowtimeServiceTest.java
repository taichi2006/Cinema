package com.cinema.showtime;

import com.cinema.common.exception.ApiException;
import com.cinema.showtime.DTO.Response.SeatMapResponse;
import com.cinema.showtime.DTO.Response.SeatResponse;
import com.cinema.showtime.DTO.Response.ShowtimeDetailResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowtimeServiceTest {

    private static class StubShowtimeDAO extends ShowtimeDAO {
        private ShowtimeInfo stubInfo;
        private List<SeatResponse> stubSeats = Collections.emptyList();

        public void setStubData(ShowtimeInfo info, List<SeatResponse> seats) {
            this.stubInfo = info;
            this.stubSeats = seats != null ? seats : Collections.emptyList();
        }

        @Override
        public Optional<ShowtimeInfo> findShowtimeInfo(Long showtimeId) {
            if (stubInfo != null && stubInfo.showtimeId().equals(showtimeId)) {
                return Optional.of(stubInfo);
            }
            return Optional.empty();
        }

        @Override
        public List<SeatResponse> findSeatsForShowtime(Long showtimeId, Long roomId, Long currentUserId) {
            return stubSeats;
        }
    }

    private StubShowtimeDAO stubDAO;
    private ShowtimeService showtimeService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        stubDAO = new StubShowtimeDAO();
        showtimeService = new ShowtimeService(stubDAO);
        objectMapper = new ObjectMapper();
    }

    private ShowtimeDAO.ShowtimeInfo createSampleShowtime(String status, Instant startsAtInstant) {
        return new ShowtimeDAO.ShowtimeInfo(
                101L,
                10L,
                "Dune: Part Two",
                1L,
                "Galaxy Nguyễn Du",
                5L,
                "Cinema 1",
                "TOP",
                startsAtInstant,
                "2026-10-05T18:00:00+07:00",
                "2026-10-05T20:30:00+07:00",
                "2D",
                "VI",
                status,
                85000L,
                "VND",
                95
        );
    }

    @Test
    @DisplayName("Lấy sơ đồ ghế thành công: trả về đủ cấu trúc showtime, serverTime, screenPosition, seats và meta rỗng")
    void testGetSeatMap_Success() throws Exception {
        Instant futureTime = Instant.now().plus(2, ChronoUnit.HOURS);
        ShowtimeDAO.ShowtimeInfo info = createSampleShowtime("OPEN", futureTime);

        SeatResponse seat1 = new SeatResponse(
                "501", "A", 1, 0, 0, "STANDARD", 85000L, "AVAILABLE", false, null
        );
        SeatResponse seat2 = new SeatResponse(
                "502", "A", 2, 1, 0, "VIP", 110000L, "HELD", true, "2026-10-05T18:05:00Z"
        );
        stubDAO.setStubData(info, List.of(seat1, seat2));

        Map<String, Object> result = showtimeService.getSeatMap("101", 1L);

        assertNotNull(result);
        assertEquals(true, result.get("success"));
        assertNotNull(result.get("data"));
        assertEquals(Collections.emptyMap(), result.get("meta"));

        SeatMapResponse seatMap = (SeatMapResponse) result.get("data");
        assertNotNull(seatMap.getServerTime());
        assertEquals("TOP", seatMap.getScreenPosition());

        ShowtimeDetailResponse showtime = seatMap.getShowtime();
        assertNotNull(showtime);
        assertEquals("101", showtime.getId());
        assertEquals("10", showtime.getMovieId());
        assertEquals("Dune: Part Two", showtime.getMovieTitle());
        assertEquals("1", showtime.getCinemaId());
        assertEquals("Galaxy Nguyễn Du", showtime.getCinemaName());
        assertEquals("5", showtime.getRoomId());
        assertEquals("Cinema 1", showtime.getRoomName());
        assertEquals("2026-10-05T18:00:00+07:00", showtime.getStartsAt());
        assertEquals("2026-10-05T20:30:00+07:00", showtime.getEndsAt());
        assertEquals("2D", showtime.getFormat());
        assertEquals("VI", showtime.getLanguage());
        assertEquals(85000L, showtime.getMinTicketPrice());
        assertEquals("VND", showtime.getCurrency());
        assertEquals(95, showtime.getAvailableSeatCount());

        List<SeatResponse> seats = seatMap.getSeats();
        assertEquals(2, seats.size());

        // Kiểm tra serialization JSON
        String json = objectMapper.writeValueAsString(result);
        JsonNode root = objectMapper.readTree(json);
        assertTrue(root.get("success").asBoolean());
        assertEquals("TOP", root.get("data").get("screenPosition").asText());
        assertEquals(2, root.get("data").get("seats").size());
    }

    @Test
    @DisplayName("Ghế đang được giữ bởi chính user: heldByCurrentUser = true và có holdExpiresAt")
    void testGetSeatMap_SeatHeldByCurrentUser() {
        Instant futureTime = Instant.now().plus(2, ChronoUnit.HOURS);
        ShowtimeDAO.ShowtimeInfo info = createSampleShowtime("OPEN", futureTime);

        SeatResponse seat = new SeatResponse(
                "502", "A", 2, 1, 0, "VIP", 110000L, "HELD", true, "2026-10-05T18:05:00Z"
        );
        stubDAO.setStubData(info, List.of(seat));

        Map<String, Object> result = showtimeService.getSeatMap("101", 1L);
        SeatMapResponse seatMap = (SeatMapResponse) result.get("data");
        SeatResponse s = seatMap.getSeats().get(0);

        assertEquals("HELD", s.getStatus());
        assertTrue(s.getHeldByCurrentUser());
        assertEquals("2026-10-05T18:05:00Z", s.getHoldExpiresAt());
    }

    @Test
    @DisplayName("Ghế đang được giữ bởi user khác: heldByCurrentUser = false và có holdExpiresAt")
    void testGetSeatMap_SeatHeldByAnotherUser() {
        Instant futureTime = Instant.now().plus(2, ChronoUnit.HOURS);
        ShowtimeDAO.ShowtimeInfo info = createSampleShowtime("OPEN", futureTime);

        SeatResponse seat = new SeatResponse(
                "502", "A", 2, 1, 0, "VIP", 110000L, "HELD", false, "2026-10-05T18:05:00Z"
        );
        stubDAO.setStubData(info, List.of(seat));

        Map<String, Object> result = showtimeService.getSeatMap("101", 2L);
        SeatMapResponse seatMap = (SeatMapResponse) result.get("data");
        SeatResponse s = seatMap.getSeats().get(0);

        assertEquals("HELD", s.getStatus());
        assertFalse(s.getHeldByCurrentUser());
        assertEquals("2026-10-05T18:05:00Z", s.getHoldExpiresAt());
    }

    @Test
    @DisplayName("Ghế đã thanh toán: status = BOOKED, heldByCurrentUser = false, holdExpiresAt = null")
    void testGetSeatMap_SeatBooked() {
        Instant futureTime = Instant.now().plus(2, ChronoUnit.HOURS);
        ShowtimeDAO.ShowtimeInfo info = createSampleShowtime("OPEN", futureTime);

        SeatResponse seat = new SeatResponse(
                "503", "A", 3, 2, 0, "STANDARD", 85000L, "BOOKED", false, null
        );
        stubDAO.setStubData(info, List.of(seat));

        Map<String, Object> result = showtimeService.getSeatMap("101", 1L);
        SeatMapResponse seatMap = (SeatMapResponse) result.get("data");
        SeatResponse s = seatMap.getSeats().get(0);

        assertEquals("BOOKED", s.getStatus());
        assertFalse(s.getHeldByCurrentUser());
        assertNull(s.getHoldExpiresAt());
    }

    @Test
    @DisplayName("Ghế bị khóa bảo trì: status = BLOCKED")
    void testGetSeatMap_SeatBlocked() {
        Instant futureTime = Instant.now().plus(2, ChronoUnit.HOURS);
        ShowtimeDAO.ShowtimeInfo info = createSampleShowtime("OPEN", futureTime);

        SeatResponse seat = new SeatResponse(
                "504", "B", 1, 0, 1, "STANDARD", 85000L, "BLOCKED", false, null
        );
        stubDAO.setStubData(info, List.of(seat));

        Map<String, Object> result = showtimeService.getSeatMap("101", 1L);
        SeatMapResponse seatMap = (SeatMapResponse) result.get("data");
        SeatResponse s = seatMap.getSeats().get(0);

        assertEquals("BLOCKED", s.getStatus());
    }

    @Test
    @DisplayName("Suất chiếu không tồn tại: ném ApiException 404 (SHOWTIME_NOT_FOUND)")
    void testGetSeatMap_ShowtimeNotFound() {
        stubDAO.setStubData(null, Collections.emptyList());

        ApiException ex = assertThrows(
                ApiException.class,
                () -> showtimeService.getSeatMap("999", 1L)
        );
        assertEquals(404, ex.getStatus());
        assertEquals("Không tìm thấy suất chiếu", ex.getMessage());
    }

    @Test
    @DisplayName("Suất chiếu không ở trạng thái OPEN (CLOSED, CANCELLED, ENDED): ném ApiException 422 (SHOWTIME_NOT_BOOKABLE)")
    void testGetSeatMap_ShowtimeNotBookable_StatusNotOpen() {
        Instant futureTime = Instant.now().plus(2, ChronoUnit.HOURS);

        // CLOSED
        ShowtimeDAO.ShowtimeInfo infoClosed = createSampleShowtime("CLOSED", futureTime);
        stubDAO.setStubData(infoClosed, Collections.emptyList());

        ApiException exClosed = assertThrows(
                ApiException.class,
                () -> showtimeService.getSeatMap("101", 1L)
        );
        assertEquals(422, exClosed.getStatus());
        assertEquals("Suất chiếu đã bắt đầu hoặc đã đóng bán", exClosed.getMessage());

        // CANCELLED
        ShowtimeDAO.ShowtimeInfo infoCancelled = createSampleShowtime("CANCELLED", futureTime);
        stubDAO.setStubData(infoCancelled, Collections.emptyList());

        ApiException exCancelled = assertThrows(
                ApiException.class,
                () -> showtimeService.getSeatMap("101", 1L)
        );
        assertEquals(422, exCancelled.getStatus());
    }

    @Test
    @DisplayName("Suất chiếu đã bắt đầu trong quá khứ: ném ApiException 422 (SHOWTIME_NOT_BOOKABLE)")
    void testGetSeatMap_ShowtimeNotBookable_PastStartTime() {
        Instant pastTime = Instant.now().minus(30, ChronoUnit.MINUTES);
        ShowtimeDAO.ShowtimeInfo info = createSampleShowtime("OPEN", pastTime);
        stubDAO.setStubData(info, Collections.emptyList());

        ApiException ex = assertThrows(
                ApiException.class,
                () -> showtimeService.getSeatMap("101", 1L)
        );
        assertEquals(422, ex.getStatus());
        assertEquals("Suất chiếu đã bắt đầu hoặc đã đóng bán", ex.getMessage());
    }

    @Test
    @DisplayName("Mã suất chiếu id không hợp lệ (null, trống, âm, 0, chữ): ném ApiException 400")
    void testGetSeatMap_InvalidShowtimeId() {
        ApiException exNull = assertThrows(
                ApiException.class,
                () -> showtimeService.getSeatMap(null, 1L)
        );
        assertEquals(400, exNull.getStatus());

        ApiException exBlank = assertThrows(
                ApiException.class,
                () -> showtimeService.getSeatMap("   ", 1L)
        );
        assertEquals(400, exBlank.getStatus());

        ApiException exNegative = assertThrows(
                ApiException.class,
                () -> showtimeService.getSeatMap("-10", 1L)
        );
        assertEquals(400, exNegative.getStatus());

        ApiException exZero = assertThrows(
                ApiException.class,
                () -> showtimeService.getSeatMap("0", 1L)
        );
        assertEquals(400, exZero.getStatus());

        ApiException exAlpha = assertThrows(
                ApiException.class,
                () -> showtimeService.getSeatMap("abc", 1L)
        );
        assertEquals(400, exAlpha.getStatus());
    }
}
