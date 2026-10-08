package com.cinema.showtime.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({
        "id",
        "seatId",
        "seatRow",
        "seatCol",
        "seatLabel",
        "price",
        "status",
        "holdExpirationAt"
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SeatResponse {

    private Long id;
    private Long seatId;
    private String seatRow;
    private Integer seatCol;
    private String seatLabel;
    private Double price;
    private String status;
    private String holdExpirationAt;

    public SeatResponse() {
    }

    public SeatResponse(
            Long id,
            Long seatId,
            String seatRow,
            Integer seatCol,
            String seatLabel,
            Double price,
            String status,
            String holdExpirationAt
    ) {
        this.id = id;
        this.seatId = seatId;
        this.seatRow = seatRow;
        this.seatCol = seatCol;
        this.seatLabel = seatLabel;
        this.price = price;
        this.status = status;
        this.holdExpirationAt = holdExpirationAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getSeatRow() {
        return seatRow;
    }

    public void setSeatRow(String seatRow) {
        this.seatRow = seatRow;
    }

    public Integer getSeatCol() {
        return seatCol;
    }

    public void setSeatCol(Integer seatCol) {
        this.seatCol = seatCol;
    }

    public String getSeatLabel() {
        return seatLabel;
    }

    public void setSeatLabel(String seatLabel) {
        this.seatLabel = seatLabel;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHoldExpirationAt() {
        return holdExpirationAt;
    }

    public void setHoldExpirationAt(String holdExpirationAt) {
        this.holdExpirationAt = holdExpirationAt;
    }
}
