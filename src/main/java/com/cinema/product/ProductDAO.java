package com.cinema.product;

import com.cinema.common.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProductDAO {

    public List<Product> findWithPaging(String category, ProductStatus status, int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT p FROM Product p WHERE 1=1");
            Map<String, Object> params = new HashMap<>();

            if (category != null && !category.isBlank()) {
                jpql.append(" AND LOWER(p.category) = LOWER(:category)");
                params.put("category", category.trim());
            }

            if (status != null) {
                jpql.append(" AND p.status = :status");
                params.put("status", status);
            }

            jpql.append(" ORDER BY p.productId ASC");

            TypedQuery<Product> query = em.createQuery(jpql.toString(), Product.class);
            params.forEach(query::setParameter);

            query.setFirstResult(page * size);
            query.setMaxResults(size);

            return query.getResultList();
        } finally {
            em.close();
        }
    }

    public long countTotal(String category, ProductStatus status) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT COUNT(p) FROM Product p WHERE 1=1");
            Map<String, Object> params = new HashMap<>();

            if (category != null && !category.isBlank()) {
                jpql.append(" AND LOWER(p.category) = LOWER(:category)");
                params.put("category", category.trim());
            }

            if (status != null) {
                jpql.append(" AND p.status = :status");
                params.put("status", status);
            }

            TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
            params.forEach(query::setParameter);

            return query.getSingleResult();
        } finally {
            em.close();
        }
    }

    public Optional<Product> findById(Long productId) {
        if (productId == null) return Optional.empty();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Product.class, productId));
        } finally {
            em.close();
        }
    }
}
