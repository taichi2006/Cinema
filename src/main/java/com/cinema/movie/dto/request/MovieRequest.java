package com.cinema.movie.dto.request;

public class MovieRequest {

    private String q;
    private String genre;
    private String status;
    private int page = 0;
    private int size = 20;
    private String sort = "releaseDate,desc";

    public MovieRequest() {
    }

    public MovieRequest(String q, String genre, String status, int page, int size, String sort) {
        this.q = q;
        this.genre = genre;
        this.status = status;
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }
}
