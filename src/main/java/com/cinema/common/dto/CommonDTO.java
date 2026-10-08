package com.cinema.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public class CommonDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiResponse<T> {
        private final boolean success;
        private final int statusCode;
        private final String message;
        private final T data;

        public ApiResponse(boolean success, int statusCode, String message, T data) {
            this.success = success;
            this.statusCode = statusCode;
            this.message = message;
            this.data = data;
        }

        public ApiResponse(boolean success, String message, T data) {
            this(success, success ? 200 : 400, message, data);
        }

        public static <T> ApiResponse<T> ok(T data) {
            return new ApiResponse<>(true, 200, null, data);
        }

        public static <T> ApiResponse<T> created(T data) {
            return new ApiResponse<>(true, 201, null, data);
        }

        public static <T> ApiResponse<T> ok(T data, String message) {
            return new ApiResponse<>(true, 200, message, data);
        }

        public static ApiResponse<Void> success(String message) {
            return new ApiResponse<>(true, 200, message, null);
        }

        public boolean isSuccess() { return success; }
        public int getStatusCode() { return statusCode; }
        public int getStatus() { return statusCode; }
        public String getMessage() { return message; }
        public T getData() { return data; }
    }

    public static class ErrorResponse {
        private final boolean success = false;
        private final int statusCode;
        private final String message;

        public ErrorResponse(int statusCode, String message) {
            this.statusCode = statusCode;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public int getStatusCode() { return statusCode; }
        public int getStatus() { return statusCode; }
        public String getMessage() { return message; }
        public String getError() { return message; }
    }

    public static class PageMeta {
        private final int page;
        private final int size;
        private final long totalElements;
        private final int totalPages;

        public PageMeta(int page, int size, long totalElements, int totalPages) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        public int getPage() { return page; }
        public int getSize() { return size; }
        public long getTotalElements() { return totalElements; }
        public int getTotalPages() { return totalPages; }
    }
}
