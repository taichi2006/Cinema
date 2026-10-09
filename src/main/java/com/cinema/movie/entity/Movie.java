package com.cinema.movie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movies", schema = "cinema")
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "director_id")
    private Long directorId;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "age_limit", nullable = false)
    private Integer ageLimit = 0;

    @Transient
    private String ageRating;

    @Column(name = "format", nullable = false, length = 50)
    private String defaultFormat = "2D";

    @Column(name = "language", nullable = false, length = 50)
    private String language = "VI";

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(name = "poster_url", columnDefinition = "text")
    private String posterUrl;

    @Transient
    private String trailerUrl;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "COMING_SOON";

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToMany
    @JoinTable(
            name = "movie_genres",
            schema = "cinema",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres = new ArrayList<>();

    public Movie() {
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public Long getDirectorId() {
        return directorId;
    }

    public void setDirectorId(Long directorId) {
        this.directorId = directorId;
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

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getAgeLimit() {
        return ageLimit;
    }

    public void setAgeLimit(Integer ageLimit) {
        this.ageLimit = ageLimit;
    }

    public String getAgeRating() {
        if (ageLimit == null || ageLimit <= 0) return "P";
        if (ageLimit < 13) return "K";
        if (ageLimit < 16) return "T13";
        if (ageLimit < 18) return "T16";
        return "T18";
    }

    public void setAgeRating(String ageRating) {
        this.ageRating = ageRating;
    }

    public String getDefaultFormat() {
        return defaultFormat;
    }

    public void setDefaultFormat(String defaultFormat) {
        this.defaultFormat = defaultFormat;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Genre> getGenres() {
        return genres;
    }

    public void setGenres(List<Genre> genres) {
        this.genres = genres;
    }
}
