package com.cinema.movie.dto.request;

public class MovieRequest {

    private String keyword;
    private String q;
    private Integer genreId;
    private String genre;
    private String status;
    private int page = 0;
    private int size = 20;
    private String sort = "releaseDate,desc";

    public MovieRequest() {
    }


    public MovieRequest(String keyword, Integer genreId, String status, int page, int size, String sort) {
        this.keyword = keyword;
        this.q = keyword;
        this.genreId = genreId;
        this.status = status;
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public String getKeyword() {
        return keyword != null ? keyword : q;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
        if (this.q == null) {
            this.q = keyword;
        }
    }

    public String getQ() {
        return q != null ? q : keyword;
    }

    public void setQ(String q) {
        this.q = q;
        if (this.keyword == null) {
            this.keyword = q;
        }
    }

    public Integer getGenreId() {
        return genreId;
    }

    public void setGenreId(Integer genreId) {
        this.genreId = genreId;
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
