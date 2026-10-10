package com.cinema.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// DTO trả về thông tin Ticket 
public class TicketResponse {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private Long ticketId;
    private Long bookingId;
    private Long showTimeSeatId;
    private String ticketCode;
    private TicketStatus status;
    private String movieName;
    private String showTimeStartTime;
    private String roomName;
    private String seatName;
    private BigDecimal price;
    private String usedAt;

    public TicketResponse() {
    }

    public TicketResponse(Ticket ticket) {
        if (ticket != null) {
            this.ticketId = ticket.getTicketId();
            this.bookingId = ticket.getBookingId();
            this.showTimeSeatId = ticket.getShowTimeSeatId();
            this.ticketCode = ticket.getTicketCode();
            this.status = ticket.getStatus();
            this.movieName = ticket.getMovieName();
            this.showTimeStartTime = formatDateTime(ticket.getShowTimeStartTime());
            this.roomName = ticket.getRoomName();
            this.seatName = ticket.getSeatName();
            this.price = ticket.getPrice();
            this.usedAt = formatDateTime(ticket.getUsedAt());
        }
    }

    private String formatDateTime(LocalDateTime dt) {
        if (dt == null) return null;
        return dt.format(ISO_FORMATTER);
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Long getShowTimeSeatId() {
        return showTimeSeatId;
    }

    public void setShowTimeSeatId(Long showTimeSeatId) {
        this.showTimeSeatId = showTimeSeatId;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getMovieName() {
        return movieName;
    }

    public void setMovieName(String movieName) {
        this.movieName = movieName;
    }

    public String getShowTimeStartTime() {
        return showTimeStartTime;
    }

    public void setShowTimeStartTime(String showTimeStartTime) {
        this.showTimeStartTime = showTimeStartTime;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getSeatName() {
        return seatName;
    }

    public void setSeatName(String seatName) {
        this.seatName = seatName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(String usedAt) {
        this.usedAt = usedAt;
    }
}
