package com.cinema.movie;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"success", "data", "meta", "traceId"})
public class SuccessEnvelope<T> {

    private boolean success = true;
    private T data;
    private PageMeta meta;
    private String traceId;

    public SuccessEnvelope() {
    }

    public SuccessEnvelope(T data, PageMeta meta) {
        this.data = data;
        this.meta = meta;
    }

    public SuccessEnvelope(T data, PageMeta meta, String traceId) {
        this.data = data;
        this.meta = meta;
        this.traceId = traceId;
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
