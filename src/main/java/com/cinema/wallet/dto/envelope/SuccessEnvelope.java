package com.cinema.wallet.dto.envelope;

/**
 * Envelope thành công chuẩn Swagger: { success: true, data: ..., meta: ..., traceId: ... }
 */
public class SuccessEnvelope<T> {

    private boolean success = true;
    private T data;
    private PageMeta meta;
    private String traceId;

    public SuccessEnvelope() {}

    public SuccessEnvelope(boolean success, T data, PageMeta meta, String traceId) {
        this.success = success;
        this.data = data;
        this.meta = meta;
        this.traceId = traceId;
    }

    public static <T> SuccessEnvelope<T> of(T data, PageMeta meta, String traceId) {
        return new SuccessEnvelope<>(true, data, meta, traceId);
    }

    public static <T> SuccessEnvelope<T> of(T data, String traceId) {
        return new SuccessEnvelope<>(true, data, null, traceId);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public PageMeta getMeta() {
        return meta;
    }

    public void setMeta(PageMeta meta) {
        this.meta = meta;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
