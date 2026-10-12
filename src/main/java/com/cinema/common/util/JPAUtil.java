package com.cinema.common.util;

import com.cinema.common.config.DB;
import jakarta.persistence.EntityManager;

public class JPAUtil {

    public static EntityManager getEntityManager() {
        return DB.getEntityManager();
    }
}
