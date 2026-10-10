package com.cinema.movie.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "showTimeId",
        "movieId",
        "roomId",
        "showDate",
        "startTime",
        "endTime",
        "basePrice",
        "status"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShowtimeResponse {

    private Long showTimeId;
    private Long movieId;
    private Long roomId;
    private String showDate;
    private String startTime;
    private String endTime;
    private Double basePrice;
    private String status;

    public ShowtimeResponse() {
    }

    public ShowtimeResponse(
            Long showTimeId,
            Long movieId,
            Long roomId,
            String showDate,
            String startTime,
            String endTime,
            Double basePrice,
            String status
    ) {
        this.showTimeId = showTimeId;
        this.movieId = movieId;
        this.roomId = roomId;
        this.showDate = showDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.basePrice = basePrice;
        this.status = status;
    }

    public Long getShowTimeId() {
        return showTimeId;
    }

    public void setShowTimeId(Long showTimeId) {
        this.showTimeId = showTimeId;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getShowDate() {
        return showDate;
    }

    public void setShowDate(String showDate) {
        this.showDate = showDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Double getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Double basePrice) {
        this.basePrice = basePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
