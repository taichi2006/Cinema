package com.cinema.movie.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "id",
        "movieId",
        "authorDisplayName",
        "rating",
        "comment",
        "createdAt"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {

    private String id;
    private String movieId;
    private String authorDisplayName;
    private Integer rating;
    private String comment;
    private String createdAt;

    public ReviewResponse() {
    }

    public ReviewResponse(
            String id,
            String movieId,
            String authorDisplayName,
            Integer rating,
            String comment,
            String createdAt
    ) {
        this.id = id;
        this.movieId = movieId;
        this.authorDisplayName = authorDisplayName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
