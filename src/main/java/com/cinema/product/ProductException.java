package com.cinema.product;

import com.cinema.common.exception.ApiException;

public class ProductException extends ApiException {

    public ProductException(int status, String error) {
        super(status, error);
    }

    public static ProductException notFound(String message) {
        return new ProductException(404, message);
    }

    public static ProductException badRequest(String message) {
        return new ProductException(400, message);
    }
}
