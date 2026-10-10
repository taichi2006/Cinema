package com.cinema.ticket;

import java.util.List;
import java.util.stream.Collectors;

public class TicketService {

    private final TicketDAO ticketDAO;

    public TicketService() {
        this.ticketDAO = new TicketDAO();
    }

    public TicketService(TicketDAO ticketDAO) {
        this.ticketDAO = ticketDAO;
    }

    // 1. GET /bookings/{bookingId}/tickets
    public List<TicketResponse> getTicketsByBooking(long currentUserId, Long bookingId) {
        if (bookingId == null || bookingId <= 0) {
            throw TicketException.notFound("Booking không tồn tại");
        }

        Long ownerId = ticketDAO.findUserIdByBookingId(bookingId)
                .orElseThrow(() -> TicketException.notFound("Booking không tồn tại"));

        if (!ownerId.equals(currentUserId)) {
            throw TicketException.forbidden("Bạn không có quyền xem vé của đơn đặt này");
        }

        List<Ticket> tickets = ticketDAO.findByBookingId(bookingId);
        return tickets.stream()
                .map(TicketResponse::new)
                .collect(Collectors.toList());
    }

    // 2. GET /tickets/{ticketId}
    public TicketResponse getTicketDetail(long currentUserId, Long ticketId) {
        if (ticketId == null || ticketId <= 0) {
            throw TicketException.notFound("Vé không tồn tại");
        }

        Ticket ticket = ticketDAO.findById(ticketId)
                .orElseThrow(() -> TicketException.notFound("Vé không tồn tại"));

        Long ownerId = ticketDAO.findUserIdByBookingId(ticket.getBookingId())
                .orElseThrow(() -> TicketException.notFound("Booking không tồn tại"));

        if (!ownerId.equals(currentUserId)) {
            throw TicketException.forbidden("Bạn không có quyền xem thông tin vé này");
        }

        return new TicketResponse(ticket);
    }
}
