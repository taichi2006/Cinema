package com.cinema.user.service;

import com.cinema.common.dto.ApiResponse;
import com.cinema.common.dto.ErrorResponse;
import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.exception.ApiException;
import com.cinema.user.criteria.UserBookingCriteria;
import com.cinema.user.dao.UserBookingDAO;
import com.cinema.user.dto.response.UserBookingResponse;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserBookingService {

    private static final Logger LOGGER = Logger.getLogger(UserBookingService.class.getName());
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final ZoneId APPLICATION_ZONE = ZoneId.of("Asia/Bangkok");
    private static final Set<String> STATUSES = Set.of(
            "PENDING", "CONFIRMED", "CANCELLED", "EXPIRED");

    private final UserBookingDAO bookingDAO;

    public UserBookingService() {
        this(new UserBookingDAO());
    }

    public UserBookingService(UserBookingDAO bookingDAO) {
        this.bookingDAO = Objects.requireNonNull(bookingDAO, "UserBookingDAO must not be null");
    }

    public PageResponse<UserBookingResponse> getBookingHistory(
            long userId,
            UserBookingCriteria query) {
        try {
            long total = bookingDAO.count(userId, query);

            var items = bookingDAO.findPage(userId, query).stream()
                    .map(row -> new UserBookingResponse(
                            row.bookingId(),
                            row.status(),
                            row.totalAmount()))
                    .toList();

            return new PageResponse<>(
                    items,
                    new PageMeta(query.page(), query.size(), total));
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Không thể tải lịch sử đặt vé", exception);
            throw ApiException.internal("Không thể tải lịch sử đặt vé");
        }
    }


}
