package com.cinema.showtime;

import jakarta.persistence.*;

@Entity
@Table(name = "seats", schema = "cinema")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Integer seatId;

    @Column(name = "room_id", nullable = false)
    private Integer roomId;

    @Column(name = "seat_type_id")
    private Integer seatTypeId;

    @Column(name = "seat_row", nullable = false, length = 10)
    private String seatRow;

    @Column(name = "seat_col", nullable = false)
    private Integer seatCol;

    @Column(name = "seat_label", length = 20)
    private String seatLabel;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    public Seat() {}

    public Integer getSeatId() { return seatId; }
    public void setSeatId(Integer seatId) { this.seatId = seatId; }

    public Integer getRoomId() { return roomId; }
    public void setRoomId(Integer roomId) { this.roomId = roomId; }

    public Integer getSeatTypeId() { return seatTypeId; }
    public void setSeatTypeId(Integer seatTypeId) { this.seatTypeId = seatTypeId; }

    public String getSeatRow() { return seatRow; }
    public void setSeatRow(String seatRow) { this.seatRow = seatRow; }

    public Integer getSeatCol() { return seatCol; }
    public void setSeatCol(Integer seatCol) { this.seatCol = seatCol; }

    public String getSeatLabel() { return seatLabel; }
    public void setSeatLabel(String seatLabel) { this.seatLabel = seatLabel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
