package com.cinema.movie;

public class MovieRequest {

    private String title;
    private Integer durationMinutes;

    public MovieRequest() {
    }

    public MovieRequest(
            String title,
            Integer durationMinutes
    ) {
        this.title = title;
        this.durationMinutes = durationMinutes;
    }

    public String getTitle() {
        return title;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }
}
