package com.cinema.cinema.dto.request;

public class CinemaRequest {

    private String city;
    private String q;
    private int page = 0;
    private int size = 20;
    private String sort = "name,asc";

    public CinemaRequest() {
    }

    public CinemaRequest(String city, String q, int page, int size, String sort) {
        this.city = city;
        this.q = q;
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
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
