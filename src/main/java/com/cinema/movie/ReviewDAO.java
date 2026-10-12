package com.cinema.movie;

import com.cinema.common.exception.ApiException;
import com.cinema.common.util.JPAUtil;
import com.cinema.movie.dto.response.ReviewResponse;
import com.cinema.ticket.TicketStatus;
import com.cinema.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    public List<ReviewResponse> findReviews(Long movieId, String sort, int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT r FROM Review r LEFT JOIN FETCH r.user WHERE r.movieId = :movieId ");
            if ("createdAt,asc".equals(sort)) {
                jpql.append("ORDER BY r.createdAt ASC");
            } else if ("rating,desc".equals(sort)) {
                jpql.append("ORDER BY r.rating DESC, r.createdAt DESC");
            } else if ("rating,asc".equals(sort)) {
                jpql.append("ORDER BY r.rating ASC, r.createdAt DESC");
            } else {
                jpql.append("ORDER BY r.createdAt DESC");
            }

            TypedQuery<Review> query = em.createQuery(jpql.toString(), Review.class)
                    .setParameter("movieId", movieId)
                    .setFirstResult(Math.max(0, page) * Math.max(1, size))
                    .setMaxResults(size);

            List<ReviewResponse> responses = new ArrayList<>();
            for (Review r : query.getResultList()) {
                String author = (r.getUser() != null && r.getUser().getFullName() != null)
                        ? r.getUser().getFullName()
                        : "Người dùng";
                responses.add(new ReviewResponse(
                        String.valueOf(r.getReviewId()),
                        String.valueOf(r.getMovieId()),
                        author,
                        r.getRating(),
                        r.getComment(),
                        r.getCreatedAt() != null ? r.getCreatedAt().toString() : null
                ));
            }
            return responses;
        } finally {
            em.close();
        }
    }

    public long countReviews(Long movieId) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT COUNT(r) FROM Review r WHERE r.movieId = :movieId", Long.class)
                    .setParameter("movieId", movieId)
                    .getSingleResult();
        } finally {
            em.close();
        }
    }

    public boolean hasPurchasedTicketForMovie(Long userId, Long movieId) {
        if (userId == null || movieId == null) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            String jpql =
                    "SELECT COUNT(b) FROM Booking b, ShowtimeSeat sts, Showtime st " +
                    "WHERE sts.bookingId = b.bookingId AND sts.showTimeId = st.showTimeId " +
                    "  AND b.userId = :userId AND st.movieId = :movieId " +
                    "  AND (UPPER(b.status) = 'CONFIRMED' OR EXISTS (" +
                    "      SELECT 1 FROM Ticket t WHERE t.bookingId = b.bookingId AND t.showTimeSeatId = sts.id " +
                    "        AND (t.status = :valid OR t.status = :used)" +
                    "  ))";
            Long count = em.createQuery(jpql, Long.class)
                    .setParameter("userId", userId)
                    .setParameter("movieId", movieId.intValue())
                    .setParameter("valid", TicketStatus.VALID)
                    .setParameter("used", TicketStatus.USED)
                    .getSingleResult();
            return count != null && count > 0L;
        } finally {
            em.close();
        }
    }

    public boolean hasUserReviewed(Long userId, Long movieId) {
        if (userId == null || movieId == null) return false;
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Long count = em.createQuery(
                    "SELECT COUNT(r) FROM Review r WHERE r.userId = :userId AND r.movieId = :movieId",
                    Long.class
            ).setParameter("userId", userId).setParameter("movieId", movieId).getSingleResult();
            return count != null && count > 0L;
        } finally {
            em.close();
        }
    }

    public ReviewResponse createReview(Long userId, Long movieId, Integer rating, String comment) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            em.getTransaction().begin();

            Review review = new Review(movieId, userId, rating, comment);
            em.persist(review);
            em.flush();

            User user = em.find(User.class, userId);
            String author = (user != null && user.getFullName() != null) ? user.getFullName() : "Người dùng";
            em.getTransaction().commit();

            return new ReviewResponse(
                    String.valueOf(review.getReviewId()),
                    String.valueOf(movieId),
                    author,
                    rating,
                    comment,
                    review.getCreatedAt() != null ? review.getCreatedAt().toString() : null
            );
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            Throwable root = e;
            while (root.getCause() != null && root.getCause() != root) {
                root = root.getCause();
            }
            String rootMsg = root.getMessage() != null ? root.getMessage().toLowerCase() : "";
            if (msg.contains("rating_check") || rootMsg.contains("rating_check")) {
                throw ApiException.badRequest("Điểm đánh giá 'rating' phải là số nguyên từ 1 đến 5 sao.");
            }
            if (msg.contains("user_id_movie_id_key") || rootMsg.contains("user_id_movie_id_key")
                    || (msg.contains("unique") && msg.contains("reviews")) || (rootMsg.contains("unique") && rootMsg.contains("reviews"))) {
                throw ApiException.conflict("Bạn đã gửi đánh giá cho bộ phim này rồi.");
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
