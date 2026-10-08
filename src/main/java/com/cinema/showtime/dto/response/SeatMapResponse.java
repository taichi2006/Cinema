package com.cinema.showtime.DTO.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({
        "showtime",
        "serverTime",
        "screenPosition",
        "seats"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatMapResponse {

    private ShowtimeDetailResponse showtime;
    private String serverTime;
    private String screenPosition;
    private List<SeatResponse> seats;

    public SeatMapResponse() {
    }

    public SeatMapResponse(
            ShowtimeDetailResponse showtime,
            String serverTime,
            String screenPosition,
            List<SeatResponse> seats
    ) {
        this.showtime = showtime;
        this.serverTime = serverTime;
        this.screenPosition = screenPosition;
        this.seats = seats;
    }

    public ShowtimeDetailResponse getShowtime() {
        return showtime;
    }

    public void setShowtime(ShowtimeDetailResponse showtime) {
        this.showtime = showtime;
    }

    public String getServerTime() {
        return serverTime;
    }

    public void setServerTime(String serverTime) {
        this.serverTime = serverTime;
    }

    public String getScreenPosition() {
        return screenPosition;
    }

    public void setScreenPosition(String screenPosition) {
        this.screenPosition = screenPosition;
    }

    public List<SeatResponse> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatResponse> seats) {
        this.seats = seats;
    }
}
