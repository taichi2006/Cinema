package com.cinema.websocket;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatSocketMessage {

    // Action types: HOLD_SEAT, RELEASE_SEAT, SEAT_STATE_CHANGED, INITIAL_STATE, PING, PONG, ERROR
    private String type;
    private Long showtimeId;
    private Long seatId;
    private Long userId;
    private String status; // AVAILABLE, HOLDING, BOOKED
    private String message;
    private Long timestamp;
    private Map<Long, Long> holdingSeats; // seatId -> userId (dùng cho INITIAL_STATE)

    public SeatSocketMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public SeatSocketMessage(String type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public static SeatSocketMessage holdChanged(Long showtimeId, Long seatId, Long userId, String status) {
        SeatSocketMessage msg = new SeatSocketMessage("SEAT_STATE_CHANGED");
        msg.setShowtimeId(showtimeId);
        msg.setSeatId(seatId);
        msg.setUserId(userId);
        msg.setStatus(status);
        return msg;
    }

    public static SeatSocketMessage initialState(Long showtimeId, Map<Long, Long> holdingSeats) {
        SeatSocketMessage msg = new SeatSocketMessage("INITIAL_STATE");
        msg.setShowtimeId(showtimeId);
        msg.setHoldingSeats(holdingSeats);
        return msg;
    }

    public static SeatSocketMessage error(String message) {
        SeatSocketMessage msg = new SeatSocketMessage("ERROR");
        msg.setMessage(message);
        return msg;
    }

    public static SeatSocketMessage pong() {
        return new SeatSocketMessage("PONG");
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getShowtimeId() { return showtimeId; }
    public void setShowtimeId(Long showtimeId) { this.showtimeId = showtimeId; }

    public Long getSeatId() { return seatId; }
    public void setSeatId(Long seatId) { this.seatId = seatId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public Map<Long, Long> getHoldingSeats() { return holdingSeats; }
    public void setHoldingSeats(Map<Long, Long> holdingSeats) { this.holdingSeats = holdingSeats; }
}
