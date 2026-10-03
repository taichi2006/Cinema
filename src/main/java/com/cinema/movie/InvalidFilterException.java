package com.cinema.movie;

public class InvalidFilterException extends RuntimeException {

    private String field;

    public InvalidFilterException(String message) {
        super(message);
    }

    public InvalidFilterException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
