package com.cinema.movie;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"success", "data", "meta"})
public class SuccessEnvelope<T> {

    private boolean success = true;
    private T data;
    private PageMeta meta;

    public SuccessEnvelope() {
    }

    public SuccessEnvelope(T data, PageMeta meta) {
        this.data = data;
        this.meta = meta;
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
}
