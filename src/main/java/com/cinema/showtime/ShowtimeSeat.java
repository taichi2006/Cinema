package com.cinema.showtime;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "show_time_seats", schema = "cinema")
public class ShowtimeSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "show_time_id", nullable = false)
    private Integer showTimeId;

    @Column(name = "seat_id", nullable = false)
    private Integer seatId;

    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "price", nullable = false)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "status", length = 20)
    private String status = "AVAILABLE"; // AVAILABLE, SELECTED, BOOKED, UNAVAILABLE

    @Column(name = "hold_expiration_at")
    private LocalDateTime holdExpirationAt;

    @Version
    @Column(name = "version")
    private Integer version = 0;

    public ShowtimeSeat() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getShowTimeId() { return showTimeId; }
    public void setShowTimeId(Integer showTimeId) { this.showTimeId = showTimeId; }

    public Integer getSeatId() { return seatId; }
    public void setSeatId(Integer seatId) { this.seatId = seatId; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getHoldExpirationAt() { return holdExpirationAt; }
    public void setHoldExpirationAt(LocalDateTime holdExpirationAt) { this.holdExpirationAt = holdExpirationAt; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
