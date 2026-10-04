package com.cinema.wallet.dto.envelope;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Envelope lỗi chuẩn Swagger: { success: false, error: { code, message, fieldErrors, details }, traceId }
 */
public record ErrorEnvelope(
        boolean success,
        ErrorDetail error,
        String traceId
) {
    public record ErrorDetail(
            String code,
            String message,
            List<FieldError> fieldErrors,
            Map<String, Object> details
    ) {}

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
}
