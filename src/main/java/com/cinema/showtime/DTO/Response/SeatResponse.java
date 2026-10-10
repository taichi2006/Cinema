package com.cinema.showtime.DTO.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "id",
        "row",
        "number",
        "x",
        "y",
        "type",
        "price",
        "status",
        "heldByCurrentUser",
        "holdExpiresAt"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatResponse {

    private String id;
    private String row;
    private Integer number;
    private Integer x;
    private Integer y;
    private String type;
    private Long price;
    private String status;
    private Boolean heldByCurrentUser;
    private String holdExpiresAt;

    public SeatResponse() {
    }

    public SeatResponse(
            String id,
            String row,
            Integer number,
            Integer x,
            Integer y,
            String type,
            Long price,
            String status,
            Boolean heldByCurrentUser,
            String holdExpiresAt
    ) {
        this.id = id;
        this.row = row;
        this.number = number;
        this.x = x;
        this.y = y;
        this.type = type;
        this.price = price;
        this.status = status;
        this.heldByCurrentUser = heldByCurrentUser;
        this.holdExpiresAt = holdExpiresAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getHeldByCurrentUser() {
        return heldByCurrentUser;
    }

    public void setHeldByCurrentUser(Boolean heldByCurrentUser) {
        this.heldByCurrentUser = heldByCurrentUser;
    }

    public String getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(String holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }
}
