package com.cinema.product.dao;

import com.cinema.common.util.JPAUtil;
import com.cinema.product.entity.Product;
import com.cinema.product.enums.ProductStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Optional;

public class ProductDAO {

    // 1. Tìm sản phẩm theo ID
    public Optional<Product> findById(Long productId) {
        if (productId == null) return Optional.empty();
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return Optional.ofNullable(em.find(Product.class, productId));
        } finally {
            em.close();
        }
    }

    // 2. Danh sách sản phẩm phân trang
    public List<Product> findWithPaging(String category, ProductStatus status, int page, int size) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT p FROM Product p WHERE 1=1");
            boolean hasCat = category != null && !category.isBlank();
            boolean hasStatus = status != null;

            if (hasCat) jpql.append(" AND LOWER(p.category) = LOWER(:cat)");
            if (hasStatus) jpql.append(" AND p.status = :st");
            jpql.append(" ORDER BY p.productId ASC");

            TypedQuery<Product> query = em.createQuery(jpql.toString(), Product.class);
            if (hasCat) query.setParameter("cat", category.trim());
            if (hasStatus) query.setParameter("st", status);

            return query.setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // 3. Đếm tổng số sản phẩm
    public long countTotal(String category, ProductStatus status) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT count(p) FROM Product p WHERE 1=1");
            boolean hasCat = category != null && !category.isBlank();
            boolean hasStatus = status != null;

            if (hasCat) jpql.append(" AND LOWER(p.category) = LOWER(:cat)");
            if (hasStatus) jpql.append(" AND p.status = :st");

            TypedQuery<Long> query = em.createQuery(jpql.toString(), Long.class);
            if (hasCat) query.setParameter("cat", category.trim());
            if (hasStatus) query.setParameter("st", status);

            return query.getSingleResult();
        } finally {
            em.close();
        }
    }
}
