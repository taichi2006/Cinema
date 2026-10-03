package com.cinema.movie;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MovieSummary {

    private Long movieId;
    private String title;
    private Integer durationMinutes;
    private LocalDate releaseDate;
    private String posterUrl;
    private String trailerUrl;
    private String ageRating;
    private String status;
    private List<String> genres = new ArrayList<>();

    public MovieSummary() {
    }

    public MovieSummary(
            Long movieId,
            String title,
            Integer durationMinutes,
            LocalDate releaseDate,
            String posterUrl,
            String trailerUrl,
            String ageRating,
            String status,
            List<String> genres
    ) {
        this.movieId = movieId;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.releaseDate = releaseDate;
        this.posterUrl = posterUrl;
        this.trailerUrl = trailerUrl;
        this.ageRating = ageRating;
        this.status = status;
        if (genres != null) {
            this.genres = genres;
        }
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getReleaseDate() {
        return releaseDate != null ? releaseDate.toString() : null;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getTrailerUrl() {
        return trailerUrl;
    }

    public void setTrailerUrl(String trailerUrl) {
        this.trailerUrl = trailerUrl;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres != null ? genres : new ArrayList<>();
    }
}
