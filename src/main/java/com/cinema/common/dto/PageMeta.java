package com.cinema.common.dto;

public class PageMeta {
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;

    public PageMeta(int page, int size, long totalElements) {
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    }

    public int getPage() {
        return page != null ? page : 0;
    }

    public int getSize() {
        return size != null ? size : 0;
    }

    public long getTotalElements() {
        return totalElements != null ? totalElements : 0L;
    }

    public Integer getTotalPages() {
        return totalPages;
    }
}
