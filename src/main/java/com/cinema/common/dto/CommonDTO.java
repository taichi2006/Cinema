package com.cinema.common.dto;

public class CommonDTO {
    public static class ApiResponse<T> {
        private final boolean success;
        private final String message;
        private final T data;

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, null, data); }
        public static <T> ApiResponse<T> ok(T data, String message) { return new ApiResponse<>(true, message, data); }
        public static ApiResponse<Void> success(String message) { return new ApiResponse<>(true, message, null); }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public T getData() { return data; }
    }

    public static class ErrorResponse {
        private final boolean success = false;
        private final int status;
        private final String error;

        public ErrorResponse(int status, String error) {
            this.status = status;
            this.error = error;
        }

        public boolean isSuccess() { return success; }
        public int getStatus() { return status; }
        public String getError() { return error; }
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
