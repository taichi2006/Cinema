package com.cinema.showtime.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({
        "showTimeId",
        "seats"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatMapResponse {

    private Long showTimeId;
    private List<SeatResponse> seats;

    public SeatMapResponse() {
    }

    public SeatMapResponse(Long showTimeId, List<SeatResponse> seats) {
        this.showTimeId = showTimeId;
        this.seats = seats;
    }

    public Long getShowTimeId() {
        return showTimeId;
    }

    public void setShowTimeId(Long showTimeId) {
        this.showTimeId = showTimeId;
    }

    public List<SeatResponse> getSeats() {
        return seats;
    }

    public void setSeats(List<SeatResponse> seats) {
        this.seats = seats;
    }
}
