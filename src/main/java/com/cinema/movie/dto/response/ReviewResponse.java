package com.cinema.movie.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "id",
        "movieId",
        "authorDisplayName",
        "rating",
        "comment",
        "version",
        "createdAt",
        "updatedAt"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {

    private String id;
    private String movieId;
    private String authorDisplayName;
    private Integer rating;
    private String comment;
    private Integer version;
    private String createdAt;
    private String updatedAt;

    public ReviewResponse() {
    }

    public ReviewResponse(
            String id,
            String movieId,
            String authorDisplayName,
            Integer rating,
            String comment,
            Integer version,
            String createdAt,
            String updatedAt
    ) {
        this.id = id;
        this.movieId = movieId;
        this.authorDisplayName = authorDisplayName;
        this.rating = rating;
        this.comment = comment;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public void setAuthorDisplayName(String authorDisplayName) {
        this.authorDisplayName = authorDisplayName;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
