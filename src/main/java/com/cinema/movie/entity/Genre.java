package com.cinema.movie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "genres", schema = "cinema")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "genre_id")
    private Long genreId;

    @Column(name = "genre_name", nullable = false, unique = true, length = 100)
    private String genreName;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Transient
    private String genreCode;

    public Genre() {
    }

    public Genre(Long genreId, String genreName, String description) {
        this.genreId = genreId;
        if (genreName != null && genreName.matches("^[A-Z0-9_]+$") && description != null && !description.matches("^[A-Z0-9_]+$")) {
            this.genreCode = genreName;
            this.genreName = description;
        } else {
            this.genreName = genreName;
            this.description = description;
        }
    }

    public Genre(Long genreId, String genreCode, String genreName, String description) {
        this.genreId = genreId;
        this.genreCode = genreCode;
        this.genreName = genreName;
        this.description = description;
    }

    public Long getGenreId() {
        return genreId;
    }

    public void setGenreId(Long genreId) {
        this.genreId = genreId;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGenreCode() {
        if (genreCode != null && !genreCode.trim().isEmpty()) {
            return genreCode;
        }
        if (genreName != null) {
            return genreName.toUpperCase().replace(" ", "_");
        }
        return genreId != null ? String.valueOf(genreId) : null;
    }

    public void setGenreCode(String genreCode) {
        this.genreCode = genreCode;
    }
}
