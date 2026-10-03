package com.cinema.movie;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;

@JsonPropertyOrder({"success", "error", "traceId"})
public class ErrorResponse {

    private boolean success = false;
    private ErrorDetail error;
    private String traceId;

    public ErrorResponse() {
    }

    public ErrorResponse(String code, String message, String traceId) {
        this.success = false;
        this.error = new ErrorDetail(code, message);
        this.traceId = traceId;
    }

    public ErrorResponse(String code, String message, List<FieldError> fieldErrors, String traceId) {
        this.success = false;
        this.error = new ErrorDetail(code, message, fieldErrors);
        this.traceId = traceId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorDetail getError() {
        return error;
    }

    public void setError(ErrorDetail error) {
        this.error = error;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    @JsonPropertyOrder({"code", "message", "fieldErrors", "details"})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {

        private String code;
        private String message;
        private List<FieldError> fieldErrors;
        private Map<String, Object> details;

        public ErrorDetail() {
        }

        public ErrorDetail(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public ErrorDetail(String code, String message, List<FieldError> fieldErrors) {
            this.code = code;
            this.message = message;
            this.fieldErrors = fieldErrors;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<FieldError> getFieldErrors() {
            return fieldErrors;
        }

        public void setFieldErrors(List<FieldError> fieldErrors) {
            this.fieldErrors = fieldErrors;
        }

        public Map<String, Object> getDetails() {
            return details;
        }

        public void setDetails(Map<String, Object> details) {
            this.details = details;
        }
    }

    @JsonPropertyOrder({"field", "message"})
    public static class FieldError {

        private String field;
        private String message;

        public FieldError() {
        }

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
