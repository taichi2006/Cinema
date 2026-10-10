package com.cinema.movie.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "reviewId",
        "movieId",
        "userId",
        "rating",
        "comment",
        "createdAt"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {

    private Long reviewId;
    private Long movieId;
    private Long userId;
    private Integer rating;
    private String comment;
    private String createdAt;

    private String id;
    private String authorDisplayName;

    public ReviewResponse() {
    }

    public ReviewResponse(
            Long reviewId,
            Long movieId,
            Long userId,
            Integer rating,
            String comment,
            String createdAt
    ) {
        this.reviewId = reviewId;
        this.id = reviewId != null ? String.valueOf(reviewId) : null;
        this.movieId = movieId;
        this.userId = userId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
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
        try {
            this.reviewId = id != null ? Long.parseLong(id) : null;
        } catch (NumberFormatException ignored) {}
        try {
            this.movieId = movieId != null ? Long.parseLong(movieId) : null;
        } catch (NumberFormatException ignored) {}
        this.authorDisplayName = authorDisplayName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
        if (this.id == null && reviewId != null) {
            this.id = String.valueOf(reviewId);
        }
    }

    public Long getMovieId() {
        return movieId;
    }

    public void setMovieId(Long movieId) {
        this.movieId = movieId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    @JsonIgnore
    public String getId() {
        return id != null ? id : (reviewId != null ? String.valueOf(reviewId) : null);
    }

    public void setId(String id) {
        this.id = id;
        try {
            if (this.reviewId == null && id != null) {
                this.reviewId = Long.parseLong(id);
            }
        } catch (NumberFormatException ignored) {}
    }

    @JsonIgnore
    public String getAuthorDisplayName() {
        return authorDisplayName;
    }

    public void setAuthorDisplayName(String authorDisplayName) {
        this.authorDisplayName = authorDisplayName;
    }
}
