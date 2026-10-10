package com.cinema.cinema.DTO.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "id",
        "movieId",
        "movieTitle",
        "cinemaId",
        "cinemaName",
        "roomId",
        "roomName",
        "startsAt",
        "endsAt",
        "format",
        "language",
        "minTicketPrice",
        "currency",
        "availableSeatCount"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CinemaShowtimeResponse {

    private String id;
    private String movieId;
    private String movieTitle;
    private String cinemaId;
    private String cinemaName;
    private String roomId;
    private String roomName;
    private String startsAt;
    private String endsAt;
    private String format;
    private String language;
    private Long minTicketPrice;
    private String currency;
    private Integer availableSeatCount;

    public CinemaShowtimeResponse() {
    }

    public CinemaShowtimeResponse(
            String id,
            String movieId,
            String movieTitle,
            String cinemaId,
            String cinemaName,
            String roomId,
            String roomName,
            String startsAt,
            String endsAt,
            String format,
            String language,
            Long minTicketPrice,
            String currency,
            Integer availableSeatCount
    ) {
        this.id = id;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.cinemaId = cinemaId;
        this.cinemaName = cinemaName;
        this.roomId = roomId;
        this.roomName = roomName;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.format = format;
        this.language = language;
        this.minTicketPrice = minTicketPrice;
        this.currency = currency;
        this.availableSeatCount = availableSeatCount;
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

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
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

    public Long getMinTicketPrice() {
        return minTicketPrice;
    }

    public void setMinTicketPrice(Long minTicketPrice) {
        this.minTicketPrice = minTicketPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getAvailableSeatCount() {
        return availableSeatCount;
    }

    public void setAvailableSeatCount(Integer availableSeatCount) {
        this.availableSeatCount = availableSeatCount;
    }
}
