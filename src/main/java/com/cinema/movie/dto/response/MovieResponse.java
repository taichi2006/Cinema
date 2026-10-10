package com.cinema.movie.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({
        "movieId",
        "title",
        "directorId",
        "durationMinutes",
        "ageLimit",
        "format",
        "description",
        "language",
        "posterUrl",
        "releaseDate",
        "status"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MovieResponse {

    private Long movieId;
    private String title;
    private Long directorId;
    private Integer durationMinutes;
    private Integer ageLimit;
    private String format;
    private String description;
    private String language;
    private String posterUrl;
    private LocalDate releaseDate;
    private String status;

    private String id;
    private String defaultFormat;
    private String trailerUrl;
    private String ageRating;
    private List<String> genres;
    private Double averageRating;
    private Integer reviewCount;

    public MovieResponse() {
    }

    public MovieResponse(
            Long movieId,
            String title,
            Long directorId,
            Integer durationMinutes,
            Integer ageLimit,
            String format,
            String description,
            String language,
            String posterUrl,
            LocalDate releaseDate,
            String status
    ) {
        this.movieId = movieId;
        this.id = movieId != null ? String.valueOf(movieId) : null;
        this.title = title;
        this.directorId = directorId;
        this.durationMinutes = durationMinutes;
        this.ageLimit = ageLimit;
        this.format = format;
        this.defaultFormat = format;
        this.description = description;
        this.language = language;
        this.posterUrl = posterUrl;
        this.releaseDate = releaseDate;
        this.status = status;
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
        try {
            this.movieId = id != null ? Long.parseLong(id) : null;
        } catch (NumberFormatException ignored) {}
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.releaseDate = releaseDate;
        this.posterUrl = posterUrl;
        this.trailerUrl = trailerUrl;
        this.language = language;
        this.format = defaultFormat;
        this.defaultFormat = defaultFormat;
        this.ageRating = ageRating;
        this.ageLimit = ageLimit;
        this.status = status;
        this.genres = genres;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
        if (this.id == null && movieId != null) {
            this.id = String.valueOf(movieId);
        }
    }

    public Long getDirectorId() {
        return directorId;
    }

    public void setDirectorId(Long directorId) {
        this.directorId = directorId;
    }

    public String getFormat() {
        return format != null ? format : defaultFormat;
    }

    public void setFormat(String format) {
        this.format = format;
        this.defaultFormat = format;
    }

    @JsonIgnore
    public String getId() {
        return id != null ? id : (movieId != null ? String.valueOf(movieId) : null);
    }

    public void setId(String id) {
        this.id = id;
        try {
            if (this.movieId == null && id != null) {
                this.movieId = Long.parseLong(id);
            }
        } catch (NumberFormatException ignored) {}
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

    @JsonIgnore
    public String getDefaultFormat() {
        return getFormat();
    }

    public void setDefaultFormat(String defaultFormat) {
        setFormat(defaultFormat);
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
        this.averageRating = averageRating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
}
