package com.cinema.cinema.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "cinemaId",
        "cinemaName",
        "address",
        "city",
        "phone",
        "email",
        "status"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CinemaResponse {

    private Long cinemaId;
    private String cinemaName;
    private String address;
    private String city;
    private String phone;
    private String email;
    private String status;

    public CinemaResponse() {
    }

    public CinemaResponse(
            Long cinemaId,
            String cinemaName,
            String address,
            String city,
            String phone,
            String email,
            String status
    ) {
        this.cinemaId = cinemaId;
        this.cinemaName = cinemaName;
        this.address = address;
        this.city = city;
        this.phone = phone;
        this.email = email;
        this.status = status;
    }

    public Long getCinemaId() {
        return cinemaId;
    }

    public void setCinemaId(Long cinemaId) {
        this.cinemaId = cinemaId;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public void setCinemaName(String cinemaName) {
        this.cinemaName = cinemaName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
