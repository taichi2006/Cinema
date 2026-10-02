package com.cinema.movie;

public class MovieDTO {

    private Long movieId;
    private String title;
    private Integer durationMinutes;

    public MovieDTO() {
    }

    public MovieDTO(
            Long movieId,
            String title,
            Integer durationMinutes
    ) {
        this.movieId = movieId;
        this.title = title;
        this.durationMinutes = durationMinutes;
    }

    public Long getMovieId() {
        return movieId;
    }

    public String getTitle() {
        return title;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }
}
