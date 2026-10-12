package com.cinema.ticket.service;

import com.cinema.common.exception.ApiException;
import com.cinema.ticket.dao.TicketDAO;
import com.cinema.ticket.dto.response.TicketResponse;
import com.cinema.ticket.entity.Ticket;
import java.util.List;

public class TicketService {

    private final TicketDAO ticketDAO = new TicketDAO();

    // 1. Danh sách vé theo bookingId
    public List<TicketResponse> getTicketsByBooking(long currentUserId, Long bookingId) {
        Long ownerId = ticketDAO.findUserIdByBookingId(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking không tồn tại"));

        if (!ownerId.equals(currentUserId)) {
            throw ApiException.forbidden("Bạn không có quyền xem vé của đơn đặt này");
        }

        return ticketDAO.findByBookingId(bookingId).stream()
                .map(TicketResponse::new)
                .toList();
    }

    // 2. Chi tiết vé
    public TicketResponse getTicketDetail(long currentUserId, Long ticketId) {
        Ticket ticket = ticketDAO.findById(ticketId)
                .orElseThrow(() -> ApiException.notFound("Vé không tồn tại"));

        Long ownerId = ticketDAO.findUserIdByBookingId(ticket.getBookingId())
                .orElseThrow(() -> ApiException.notFound("Booking không tồn tại"));

        if (!ownerId.equals(currentUserId)) {
            throw ApiException.forbidden("Bạn không có quyền xem thông tin vé này");
        }

        return new TicketResponse(ticket);
    }
}
