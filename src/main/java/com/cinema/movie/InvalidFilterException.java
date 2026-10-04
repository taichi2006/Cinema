package com.cinema.movie;

import com.cinema.common.exception.ApiException;

public class InvalidFilterException extends ApiException {

    private String field;

    public InvalidFilterException(String message) {
        super(400, message);
    }

    public InvalidFilterException(String field, String message) {
        super(400, message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
