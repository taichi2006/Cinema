package com.cinema.movie.DTO.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "id",
        "movieId",
        "cinemaId",
        "cinemaName",
        "roomId",
        "roomName",
        "startsAt",
        "endsAt",
        "format",
        "language",
        "basePrice",
        "status"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShowtimeResponse {

    private String id;
    private String movieId;
    private String cinemaId;
    private String cinemaName;
    private String roomId;
    private String roomName;
    private String startsAt;
    private String endsAt;
    private String format;
    private String language;
    private Long basePrice;
    private String status;

    public ShowtimeResponse() {
    }

    public ShowtimeResponse(
            String id,
            String movieId,
            String cinemaId,
            String cinemaName,
            String roomId,
            String roomName,
            String startsAt,
            String endsAt,
            String format,
            String language,
            Long basePrice,
            String status
    ) {
        this.id = id;
        this.movieId = movieId;
        this.cinemaId = cinemaId;
        this.cinemaName = cinemaName;
        this.roomId = roomId;
        this.roomName = roomName;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.format = format;
        this.language = language;
        this.basePrice = basePrice;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMovieId() {
        return movieId;
    }

    public void setMovieId(String movieId) {
        this.movieId = movieId;
    }

    public String getCinemaId() {
        return cinemaId;
    }

    public void setCinemaId(String cinemaId) {
        this.cinemaId = cinemaId;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public void setCinemaName(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(String startsAt) {
        this.startsAt = startsAt;
    }

    public String getEndsAt() {
        return endsAt;
    }

    public void setEndsAt(String endsAt) {
        this.endsAt = endsAt;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Long getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Long basePrice) {
        this.basePrice = basePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
