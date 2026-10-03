package com.cinema.movie;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({
        "id",
        "title",
        "posterUrl",
        "durationMinutes",
        "releaseDate",
        "genres",
        "status",
        "ageRating",
        "averageRating",
        "reviewCount"
})
public class MovieSummary {

    private String id;
    private String title;
    private String posterUrl;
    private Integer durationMinutes;
    private LocalDate releaseDate;
    private List<String> genres = new ArrayList<>();
    private String status;
    private String ageRating;
    private Double averageRating = 0.0;
    private Integer reviewCount = 0;

    public MovieSummary() {
    }

    public MovieSummary(
            String id,
            String title,
            String posterUrl,
            Integer durationMinutes,
            LocalDate releaseDate,
            List<String> genres,
            String status,
            String ageRating,
            Double averageRating,
            Integer reviewCount
    ) {
        this.id = id;
        this.title = title;
        this.posterUrl = posterUrl;
        this.durationMinutes = durationMinutes;
        this.releaseDate = releaseDate;
        if (genres != null) {
            this.genres = genres;
        }
        this.status = status;
        this.ageRating = ageRating;
        this.averageRating = averageRating != null ? averageRating : 0.0;
        this.reviewCount = reviewCount != null ? reviewCount : 0;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
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

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres != null ? genres : new ArrayList<>();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating != null ? averageRating : 0.0;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount != null ? reviewCount : 0;
    }
}
