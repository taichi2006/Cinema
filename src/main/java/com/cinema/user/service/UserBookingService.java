package com.cinema.user.service;

import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.user.dao.UserBookingDAO;
import com.cinema.user.dto.response.UserBookingResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class UserBookingService {

    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final UserBookingDAO bookingDAO = new UserBookingDAO();

    // Lịch sử đặt vé của người dùng
    public PageResponse<UserBookingResponse> getBookingHistory(
            long userId, String status, LocalDate from, LocalDate to, int page, int size, String sort) {

        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("from không được lớn hơn to");
        }

        int p = Math.max(0, page);
        int s = (size < 1 || size > 100) ? 20 : size;

        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;
        Instant fromInstant = from == null ? null : from.atStartOfDay(APPLICATION_ZONE).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(APPLICATION_ZONE).toInstant();
        String sortDirection = ("asc".equalsIgnoreCase(sort) || "createdAt,asc".equalsIgnoreCase(sort)) ? "ASC" : "DESC";

        long total = bookingDAO.count(userId, cleanStatus, fromInstant, toInstant);
        List<UserBookingResponse> items = bookingDAO.findPage(userId, cleanStatus, fromInstant, toInstant, p, s, sortDirection).stream()
                .map(row -> new UserBookingResponse(row.bookingId(), row.status(), row.totalAmount()))
                .toList();

        return PageResponse.of(items, new PageMeta(p, s, total));
    }
}
