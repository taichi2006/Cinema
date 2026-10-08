package com.cinema.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public class CommonDTO {
    public static class ApiResponse<T> {
        private final boolean success;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final String message;

        @JsonInclude(JsonInclude.Include.ALWAYS)
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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageMeta {
        private final Integer page;
        private final Integer size;
        private final Long totalElements;
        private final Integer totalPages;

        public PageMeta(int page, int size, long totalElements) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = null;
        }

        public PageMeta(int page, int size, long totalElements, int totalPages) {
            this.page = page;
            this.size = size;
            this.totalElements = totalElements;
            this.totalPages = totalPages;
        }

        public int getPage() { return page != null ? page : 0; }
        public int getSize() { return size != null ? size : 0; }
        public long getTotalElements() { return totalElements != null ? totalElements : 0L; }
        public Integer getTotalPages() { return totalPages; }
    }
}
