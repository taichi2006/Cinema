package com.cinema.movie;

import com.cinema.common.util.JPAUtil;

import jakarta.persistence.EntityManager;

import java.util.List;

public class MovieDAO {

    public List<Movie> findAll() {

        EntityManager entityManager =
                JPAUtil.getEntityManager();

        try {

            return entityManager
                    .createQuery(
                            "SELECT movie FROM Movie movie",
                            Movie.class
                    )
                    .getResultList();

        } finally {

            entityManager.close();
        }
    }
}
