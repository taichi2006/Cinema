package com.cinema.ticket;

import com.cinema.common.exception.ApiException;

public class TicketException extends ApiException {

    public TicketException(int status, String error) {
        super(status, error);
    }

    public static TicketException notFound(String message) {
        return new TicketException(404, message);
    }

    public static TicketException forbidden(String message) {
        return new TicketException(403, message);
    }

    public static TicketException unauthorized(String message) {
        return new TicketException(401, message);
    }
}
