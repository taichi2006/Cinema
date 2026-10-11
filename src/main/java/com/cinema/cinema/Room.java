package com.cinema.cinema;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms", schema = "cinema")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Integer roomId;

    @Column(name = "cinema_id", nullable = false)
    private Long cinemaId;

    @Column(name = "room_type_id")
    private Integer roomTypeId;

    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;

    @Transient
    private String screenPosition = "TOP";

    @Transient
    private Integer totalSeats;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id", insertable = false, updatable = false)
    private Cinema cinema;

    public Room() {}

    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }

    public Long getCinemaId() { return cinemaId; }
    public void setCinemaId(Long cinemaId) { this.cinemaId = cinemaId; }

    public Integer getRoomTypeId() { return roomTypeId; }
    public void setRoomTypeId(Integer roomTypeId) { this.roomTypeId = roomTypeId; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getScreenPosition() { return screenPosition; }
    public void setScreenPosition(String screenPosition) { this.screenPosition = screenPosition; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Cinema getCinema() { return cinema; }
    public void setCinema(Cinema cinema) { this.cinema = cinema; }
}
