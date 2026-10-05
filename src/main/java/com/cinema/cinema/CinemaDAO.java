package com.cinema.cinema;

import com.cinema.cinema.DTO.Request.CinemaRequest;
import com.cinema.common.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CinemaDAO {

    public Optional<Cinema> findById(Long cinemaId) {
        if (cinemaId == null) {
            return Optional.empty();
        }
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            Cinema cinema = entityManager.find(Cinema.class, cinemaId);
            return Optional.ofNullable(cinema);
        } finally {
            entityManager.close();
        }
    }

    public List<Cinema> findCinemas(CinemaRequest filter) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT c FROM Cinema c WHERE c.status = 'ACTIVE' ");
            Map<String, Object> params = new HashMap<>();

            if (filter.getCity() != null && !filter.getCity().trim().isEmpty()) {
                jpql.append("AND (LOWER(c.cityCode) = :city OR LOWER(c.cityName) = :city) ");
                params.put("city", filter.getCity().trim().toLowerCase());
            }

            if (filter.getQ() != null && !filter.getQ().trim().isEmpty()) {
                jpql.append("AND (LOWER(c.cinemaName) LIKE :q OR LOWER(c.address) LIKE :q) ");
                params.put("q", "%" + filter.getQ().trim().toLowerCase() + "%");
            }

            if ("name,desc".equalsIgnoreCase(filter.getSort())) {
                jpql.append("ORDER BY c.cinemaName DESC, c.cinemaId ASC");
            } else {
                jpql.append("ORDER BY c.cinemaName ASC, c.cinemaId ASC");
            }

            TypedQuery<Cinema> query = entityManager.createQuery(jpql.toString(), Cinema.class);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            int offset = Math.max(0, filter.getPage()) * Math.max(1, filter.getSize());
            query.setFirstResult(offset);
            query.setMaxResults(filter.getSize());

            return query.getResultList();
        } finally {
            entityManager.close();
        }
    }

    public long countCinemas(CinemaRequest filter) {
        EntityManager entityManager = JPAUtil.getEntityManager();
        try {
            StringBuilder jpql = new StringBuilder("SELECT COUNT(c) FROM Cinema c WHERE c.status = 'ACTIVE' ");
            Map<String, Object> params = new HashMap<>();

            if (filter.getCity() != null && !filter.getCity().trim().isEmpty()) {
                jpql.append("AND (LOWER(c.cityCode) = :city OR LOWER(c.cityName) = :city) ");
                params.put("city", filter.getCity().trim().toLowerCase());
            }

            if (filter.getQ() != null && !filter.getQ().trim().isEmpty()) {
                jpql.append("AND (LOWER(c.cinemaName) LIKE :q OR LOWER(c.address) LIKE :q) ");
                params.put("q", "%" + filter.getQ().trim().toLowerCase() + "%");
            }

            TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            return query.getSingleResult();
        } finally {
            entityManager.close();
        }
    }
}
