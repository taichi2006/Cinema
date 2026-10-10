package com.cinema.ticket;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets", schema = "cinema")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_id")
    private Long ticketId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "show_time_seat_id", nullable = false, unique = true)
    private Long showTimeSeatId;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 500)
    private String ticketCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TicketStatus status = TicketStatus.VALID;

    @Column(name = "movie_name", length = 255)
    private String movieName;

    @Column(name = "show_time_start_time")
    private LocalDateTime showTimeStartTime;

    @Column(name = "room_name", length = 100)
    private String roomName;

    @Column(name = "seat_name", length = 20)
    private String seatName;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public Ticket() {
    }

    public Ticket(Long bookingId, Long showTimeSeatId, String ticketCode, TicketStatus status,
                  String movieName, LocalDateTime showTimeStartTime, String roomName,
                  String seatName, BigDecimal price) {
        this.bookingId = bookingId;
        this.showTimeSeatId = showTimeSeatId;
        this.ticketCode = ticketCode;
        this.status = status != null ? status : TicketStatus.VALID;
        this.movieName = movieName;
        this.showTimeStartTime = showTimeStartTime;
        this.roomName = roomName;
        this.seatName = seatName;
        this.price = price;
    }

    // Getters and Setters
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

    public LocalDateTime getShowTimeStartTime() {
        return showTimeStartTime;
    }

    public void setShowTimeStartTime(LocalDateTime showTimeStartTime) {
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

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
