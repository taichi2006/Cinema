package com.cinema.wallet.dto.envelope;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Envelope lỗi chuẩn Swagger: { success: false, error: { code, message, fieldErrors, details }, traceId }
 */
public class ErrorEnvelope {

    private boolean success = false;
    private ErrorDetail error;
    private String traceId;

    public ErrorEnvelope() {}

    public ErrorEnvelope(boolean success, ErrorDetail error, String traceId) {
        this.success = success;
        this.error = error;
        this.traceId = traceId;
    }

    public static class ErrorDetail {
        private String code;
        private String message;
        private List<FieldError> fieldErrors;
        private Map<String, Object> details;

        public ErrorDetail() {}

        public ErrorDetail(String code, String message, List<FieldError> fieldErrors, Map<String, Object> details) {
            this.code = code;
            this.message = message;
            this.fieldErrors = fieldErrors != null ? fieldErrors : Collections.emptyList();
            this.details = details != null ? details : Collections.emptyMap();
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

    public static ErrorEnvelope of(String code, String message, List<FieldError> fieldErrors, String traceId) {
        return new ErrorEnvelope(
                false,
                new ErrorDetail(code, message, fieldErrors != null ? fieldErrors : Collections.emptyList(), Collections.emptyMap()),
                traceId
        );
    }

    public static ErrorEnvelope of(String code, String message, String traceId) {
        return of(code, message, Collections.emptyList(), traceId);
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
}
