package com.cinema.ticket;

import com.cinema.common.dto.CommonDTO.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TicketModuleTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("Ticket entity mapping đúng các thuộc tính")
    void testTicketEntityMapping() {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = new Ticket(1001L, 5001L, "TKT-A1B2-C3D4", TicketStatus.VALID,
                "Inception", now, "Room 1", "A1", BigDecimal.valueOf(90000));
        ticket.setTicketId(9001L);

        assertEquals(9001L, ticket.getTicketId());
        assertEquals(1001L, ticket.getBookingId());
        assertEquals(5001L, ticket.getShowTimeSeatId());
        assertEquals("TKT-A1B2-C3D4", ticket.getTicketCode());
        assertEquals(TicketStatus.VALID, ticket.getStatus());
        assertEquals("Inception", ticket.getMovieName());
        assertEquals(now, ticket.getShowTimeStartTime());
        assertEquals("Room 1", ticket.getRoomName());
        assertEquals("A1", ticket.getSeatName());
        assertEquals(BigDecimal.valueOf(90000), ticket.getPrice());
    }

    @Test
    @DisplayName("TicketResponse DTO serialize đúng cấu trúc JSON Swagger")
    void testTicketResponseSerialization() throws Exception {
        LocalDateTime now = LocalDateTime.of(2024, 7, 15, 19, 0, 0);
        Ticket ticket = new Ticket(1001L, 5001L, "TKT-A1B2-C3D4", TicketStatus.VALID,
                "Inception", now, "Room 1", "A1", BigDecimal.valueOf(90000));
        ticket.setTicketId(9001L);

        TicketResponse response = new TicketResponse(ticket);
        ApiResponse<TicketResponse> apiResponse = ApiResponse.ok(response);

        String jsonStr = json.writeValueAsString(apiResponse);

        assertTrue(jsonStr.contains("\"ticketId\":9001"));
        assertTrue(jsonStr.contains("\"bookingId\":1001"));
        assertTrue(jsonStr.contains("\"ticketCode\":\"TKT-A1B2-C3D4\""));
        assertTrue(jsonStr.contains("\"status\":\"VALID\""));
        assertTrue(jsonStr.contains("\"movieName\":\"Inception\""));
        assertTrue(jsonStr.contains("\"seatName\":\"A1\""));
        assertTrue(jsonStr.contains("\"success\":true"));
    }

    @Test
    @DisplayName("TicketService: Lấy danh sách vé của booking thành công khi đúng chủ sở hữu")
    void testGetTicketsByBookingSuccess() {
        long currentUserId = 10L;
        long bookingId = 1001L;

        Ticket t1 = new Ticket(bookingId, 5001L, "TKT-1", TicketStatus.VALID, "Inception", null, "Room 1", "A1", BigDecimal.valueOf(90000));
        t1.setTicketId(1L);
        Ticket t2 = new Ticket(bookingId, 5002L, "TKT-2", TicketStatus.VALID, "Inception", null, "Room 1", "A2", BigDecimal.valueOf(90000));
        t2.setTicketId(2L);

        TicketDAO fakeDAO = new TicketDAO() {
            @Override
            public Optional<Long> findUserIdByBookingId(Long bId) {
                return Optional.of(10L); // đúng currentUserId
            }

            @Override
            public List<Ticket> findByBookingId(Long bId) {
                return List.of(t1, t2);
            }
        };

        TicketService service = new TicketService(fakeDAO);
        List<TicketResponse> result = service.getTicketsByBooking(currentUserId, bookingId);

        assertEquals(2, result.size());
        assertEquals("TKT-1", result.get(0).getTicketCode());
        assertEquals("TKT-2", result.get(1).getTicketCode());
    }

    @Test
    @DisplayName("TicketService: Ném 403 Forbidden khi user không phải chủ sở hữu booking")
    void testGetTicketsByBookingForbidden() {
        long currentUserId = 10L;
        long bookingId = 1001L;

        TicketDAO fakeDAO = new TicketDAO() {
            @Override
            public Optional<Long> findUserIdByBookingId(Long bId) {
                return Optional.of(99L); // thuộc sở hữu user khác
            }
        };

        TicketService service = new TicketService(fakeDAO);
        TicketException ex = assertThrows(TicketException.class, () ->
                service.getTicketsByBooking(currentUserId, bookingId));

        assertEquals(403, ex.getStatus());
        assertTrue(ex.getMessage().contains("Bạn không có quyền"));
    }

    @Test
    @DisplayName("TicketService: Ném 404 Not Found khi booking không tồn tại")
    void testGetTicketsByBookingNotFound() {
        TicketDAO fakeDAO = new TicketDAO() {
            @Override
            public Optional<Long> findUserIdByBookingId(Long bId) {
                return Optional.empty();
            }
        };

        TicketService service = new TicketService(fakeDAO);
        TicketException ex = assertThrows(TicketException.class, () ->
                service.getTicketsByBooking(10L, 9999L));

        assertEquals(404, ex.getStatus());
        assertTrue(ex.getMessage().contains("Booking không tồn tại"));
    }

    @Test
    @DisplayName("TicketService: Lấy chi tiết vé thành công khi đúng chủ sở hữu")
    void testGetTicketDetailSuccess() {
        Ticket ticket = new Ticket(1001L, 5001L, "TKT-A1", TicketStatus.VALID, "Dune 2", null, "Room 2", "B5", BigDecimal.valueOf(100000));
        ticket.setTicketId(77L);

        TicketDAO fakeDAO = new TicketDAO() {
            @Override
            public Optional<Ticket> findById(Long ticketId) {
                return Optional.of(ticket);
            }

            @Override
            public Optional<Long> findUserIdByBookingId(Long bId) {
                return Optional.of(10L); // đúng currentUserId
            }
        };

        TicketService service = new TicketService(fakeDAO);
        TicketResponse result = service.getTicketDetail(10L, 77L);

        assertNotNull(result);
        assertEquals(77L, result.getTicketId());
        assertEquals("TKT-A1", result.getTicketCode());
        assertEquals("Dune 2", result.getMovieName());
    }

    @Test
    @DisplayName("TicketService: Ném 404 khi vé không tồn tại")
    void testGetTicketDetailNotFound() {
        TicketDAO fakeDAO = new TicketDAO() {
            @Override
            public Optional<Ticket> findById(Long ticketId) {
                return Optional.empty();
            }
        };

        TicketService service = new TicketService(fakeDAO);
        TicketException ex = assertThrows(TicketException.class, () ->
                service.getTicketDetail(10L, 9999L));

        assertEquals(404, ex.getStatus());
        assertTrue(ex.getMessage().contains("Vé không tồn tại"));
    }
}
