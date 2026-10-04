package com.cinema.movie.DTO.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({
        "id",
        "title",
        "description",
        "durationMinutes",
        "releaseDate",
        "posterUrl",
        "trailerUrl",
        "language",
        "defaultFormat",
        "ageRating",
        "ageLimit",
        "status",
        "genres",
        "averageRating",
        "reviewCount"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovieResponse {

    private String id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private LocalDate releaseDate;
    private String posterUrl;
    private String trailerUrl;
    private String language;
    private String defaultFormat;
    private String ageRating;
    private Integer ageLimit;
    private String status;
    private List<String> genres = new ArrayList<>();
    private Double averageRating = 0.0;
    private Integer reviewCount = 0;

    public MovieResponse() {
    }

    public MovieResponse(
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
        this(id, title, null, durationMinutes, releaseDate, posterUrl, null, null, null, ageRating, null, status, genres, averageRating, reviewCount);
    }

    public MovieResponse(
            String id,
            String title,
            String description,
            Integer durationMinutes,
            LocalDate releaseDate,
            String posterUrl,
            String trailerUrl,
            String language,
            String defaultFormat,
            String ageRating,
            Integer ageLimit,
            String status,
            List<String> genres,
            Double averageRating,
            Integer reviewCount
    ) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.releaseDate = releaseDate;
        this.posterUrl = posterUrl;
        this.trailerUrl = trailerUrl;
        this.language = language;
        this.defaultFormat = defaultFormat;
        this.ageRating = ageRating;
        this.ageLimit = ageLimit;
        this.status = status;
        if (genres != null) {
            this.genres = genres;
        }
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDefaultFormat() {
        return defaultFormat;
    }

    public void setDefaultFormat(String defaultFormat) {
        this.defaultFormat = defaultFormat;
    }

    public String getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public Integer getAgeLimit() {
        return ageLimit;
    }

    public void setAgeLimit(Integer ageLimit) {
        this.ageLimit = ageLimit;
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
