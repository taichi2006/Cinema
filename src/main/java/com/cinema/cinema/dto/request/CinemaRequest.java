package com.cinema.cinema.dto.request;

public class CinemaRequest {

    private String city;
    private String status;
    private int page = 0;
    private int size = 20;

    public CinemaRequest() {
    }

    public CinemaRequest(String city, String status, int page, int size) {
        this.city = city;
        this.status = status;
        this.page = page;
        this.size = size;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
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
}
