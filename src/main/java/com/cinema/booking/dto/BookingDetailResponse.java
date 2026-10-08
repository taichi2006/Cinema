package com.cinema.booking.dto;

import java.math.BigDecimal;
import java.util.List;

public class BookingDetailResponse {
    private Integer bookingId;
    private ShowTimeSummary showTime;
    private List<String> seats;
    private List<ProductSummary> products;
    private BigDecimal totalAmount;
    private String status;

    public static class ShowTimeSummary {
        private Integer showTimeId;
        private String movieTitle;
        private String startTime;

        public ShowTimeSummary() {}

        public ShowTimeSummary(Integer showTimeId, String movieTitle, String startTime) {
            this.showTimeId = showTimeId;
            this.movieTitle = movieTitle;
            this.startTime = startTime;
        }

        public Integer getShowTimeId() { return showTimeId; }
        public void setShowTimeId(Integer showTimeId) { this.showTimeId = showTimeId; }

        public String getMovieTitle() { return movieTitle; }
        public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }

        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
    }

    public static class ProductSummary {
        private String name;
        private Integer quantity;
        private BigDecimal price;

        public ProductSummary() {}

        public ProductSummary(String name, Integer quantity, BigDecimal price) {
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
    }

    public BookingDetailResponse() {}

    public BookingDetailResponse(Integer bookingId, ShowTimeSummary showTime, List<String> seats, List<ProductSummary> products, BigDecimal totalAmount, String status) {
        this.bookingId = bookingId;
        this.showTime = showTime;
        this.seats = seats;
        this.products = products;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public Integer getBookingId() { return bookingId; }
    public void setBookingId(Integer bookingId) { this.bookingId = bookingId; }

    public ShowTimeSummary getShowTime() { return showTime; }
    public void setShowTime(ShowTimeSummary showTime) { this.showTime = showTime; }

    public List<String> getSeats() { return seats; }
    public void setSeats(List<String> seats) { this.seats = seats; }

    public List<ProductSummary> getProducts() { return products; }
    public void setProducts(List<ProductSummary> products) { this.products = products; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
