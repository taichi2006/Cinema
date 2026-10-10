package com.cinema.common.dto;

import java.util.List;

public class PageResponse<T> {
    private List<T> items;
    private PageMeta meta;

    public PageResponse(List<T> items, PageMeta meta) {
        this.items = items;
        this.meta = meta;
    }

    public static <T> PageResponse<T> of(List<T> items, PageMeta meta) {
        return new PageResponse<>(items, meta);
    }

    public List<T> getItems() {
        return items;
    }

    public PageMeta getMeta() {
        return meta;
    }
}
