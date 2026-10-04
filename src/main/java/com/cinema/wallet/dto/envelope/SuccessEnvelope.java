package com.cinema.wallet.dto.envelope;

/**
 * Envelope thành công chuẩn Swagger: { success: true, data: ..., meta: ..., traceId: ... }
 */
public record SuccessEnvelope<T>(
        boolean success,
        T data,
        PageMeta meta,
        String traceId
) {
    public static <T> SuccessEnvelope<T> of(T data, PageMeta meta, String traceId) {
        return new SuccessEnvelope<>(true, data, meta, traceId);
    }

    public static <T> SuccessEnvelope<T> of(T data, String traceId) {
        return new SuccessEnvelope<>(true, data, null, traceId);
    }
}
