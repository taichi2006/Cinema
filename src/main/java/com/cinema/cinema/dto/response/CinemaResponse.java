package com.cinema.cinema.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "id",
        "name",
        "cityCode",
        "cityName",
        "address",
        "phone",
        "imageUrl",
        "latitude",
        "longitude"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CinemaResponse {

    private String id;
    private String name;
    private String cityCode;
    private String cityName;
    private String address;
    private String phone;
    private String imageUrl;
    private Double latitude;
    private Double longitude;

    public CinemaResponse() {
    }

    public CinemaResponse(
            String id,
            String name,
            String cityCode,
            String cityName,
            String address,
            String phone,
            String imageUrl,
            Double latitude,
            Double longitude
    ) {
        this.id = id;
        this.name = name;
        this.cityCode = cityCode;
        this.cityName = cityName;
        this.address = address;
        this.phone = phone;
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
