package com.cinema.movie;

public class SuccessEnvelope<T> {

    private T data;
    private PageMeta meta;

    public SuccessEnvelope() {
    }

    public SuccessEnvelope(T data, PageMeta meta) {
        this.data = data;
        this.meta = meta;
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
