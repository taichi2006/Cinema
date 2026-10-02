package com.cinema.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {

    private static final Dotenv dotenv = Dotenv.load();

    private static final String URL =
            dotenv.get("DB_URL");

    private static final String USERNAME =
            dotenv.get("DB_USERNAME");

    private static final String PASSWORD =
            dotenv.get("DB_PASSWORD");

    public static Connection getConnection() throws SQLException {

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException(
                    "PostgreSQL Driver not found",
                    exception
            );
        }

        return DriverManager.getConnection(
                URL,
                USERNAME,
                PASSWORD
        );
    }
}