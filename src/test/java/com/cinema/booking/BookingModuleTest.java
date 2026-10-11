package com.cinema.booking;

import com.cinema.booking.dto.*;
import com.cinema.booking.entity.Booking;
import com.cinema.common.dto.ApiResponse;
import com.cinema.common.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BookingModuleTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("CreateBookingResponse serialization test")
    void testCreateBookingResponseSerialization() throws Exception {
        CreateBookingResponse response = new CreateBookingResponse(
                100L,
                1,
                List.of(10, 11),
                BigDecimal.valueOf(180000),
                "2026-10-11T12:00:00Z",
                "PENDING"
        );

        ApiResponse<CreateBookingResponse> apiResponse = ApiResponse.ok(response);
        String jsonStr = json.writeValueAsString(apiResponse);

        assertTrue(jsonStr.contains("\"bookingId\":100"));
        assertTrue(jsonStr.contains("\"status\":\"PENDING\""));
        assertTrue(jsonStr.contains("\"success\":true"));
    }

    @Test
    @DisplayName("BookingService: createBooking validation fails when request is empty or missing fields")
    void testCreateBookingValidation() {
        BookingService service = new BookingService(new BookingDAO());

        assertThrows(ApiException.class, () -> service.createBooking(1L, null));
        assertThrows(ApiException.class, () -> service.createBooking(1L, new CreateBookingRequest(null, List.of(1), null)));
        assertThrows(ApiException.class, () -> service.createBooking(1L, new CreateBookingRequest(1, null, null)));
        assertThrows(ApiException.class, () -> service.createBooking(1L, new CreateBookingRequest(1, List.of(), null)));
    }

    @Test
    @DisplayName("BookingService: getBookingDetail invalid bookingId")
    void testGetBookingDetailInvalidId() {
        BookingService service = new BookingService(new BookingDAO());

        ApiException ex1 = assertThrows(ApiException.class, () -> service.getBookingDetail(null, 1L));
        assertEquals(400, ex1.getStatus());

        ApiException ex2 = assertThrows(ApiException.class, () -> service.getBookingDetail(-1L, 1L));
        assertEquals(400, ex2.getStatus());
    }

    @Test
    @DisplayName("BookingService: updateBookingItems and applyVoucher validation")
    void testUpdateBookingItemsAndVoucherValidation() {
        BookingService service = new BookingService(new BookingDAO());

        assertThrows(ApiException.class, () -> service.updateBookingItems(null, 1L, new UpdateBookingItemsRequest()));
        assertThrows(ApiException.class, () -> service.updateBookingItems(1L, 1L, null));

        assertThrows(ApiException.class, () -> service.applyVoucher(null, 1L, new ApplyVoucherRequest("VOUCHER10")));
        assertThrows(ApiException.class, () -> service.applyVoucher(1L, 1L, null));
        assertThrows(ApiException.class, () -> service.applyVoucher(1L, 1L, new ApplyVoucherRequest("")));
    }

    @Test
    @DisplayName("BookingService: cancelBooking and refund validation")
    void testCancelAndRefundValidation() {
        BookingService service = new BookingService(new BookingDAO());

        assertThrows(ApiException.class, () -> service.cancelBooking(null, 1L, null));
        assertThrows(ApiException.class, () -> service.createRefund(null, 1L, null));
        assertThrows(ApiException.class, () -> service.getRefundDetail(null, 1L));
    }

    @Test
    @DisplayName("Booking entity basic properties")
    void testBookingEntity() {
        Booking booking = new Booking();
        booking.setBookingId(55L);
        booking.setUserId(2L);
        booking.setStatus("PENDING");
        booking.setTotalAmount(BigDecimal.valueOf(120000));

        assertEquals(55L, booking.getBookingId());
        assertEquals(2L, booking.getUserId());
        assertEquals("PENDING", booking.getStatus());
        assertEquals(BigDecimal.valueOf(120000), booking.getTotalAmount());
    }
}
