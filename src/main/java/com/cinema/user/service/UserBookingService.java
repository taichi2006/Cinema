package com.cinema.user.service;

import com.cinema.common.dto.CommonDTO;
import com.cinema.common.exception.ApiException;
import com.cinema.user.criteria.UserBookingCriteria;
import com.cinema.user.dao.UserBookingDAO;
import com.cinema.user.dto.request.UserBookingRequest;
import com.cinema.user.dto.response.UserBookingResponse;
import com.cinema.user.dto.response.UserPageResponse;

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
            "PENDING", "CONFIRMED", "CANCELLED", "EXPIRED"
    );

    private final UserBookingDAO bookingDAO;

    public UserBookingService() {
        this(new UserBookingDAO());
    }

    public UserBookingService(UserBookingDAO bookingDAO) {
        this.bookingDAO = Objects.requireNonNull(bookingDAO, "UserBookingDAO must not be null");
    }

    public UserPageResponse<UserBookingResponse> getBookingHistory(
            long userId,
            UserBookingRequest request
    ) {
        UserBookingCriteria query = validateAndCreateCriteria(request);
        try {
            long total = bookingDAO.count(userId, query);
            var items = bookingDAO.findPage(userId, query).stream()
                    .map(row -> new UserBookingResponse(
                            row.bookingId(),
                            row.status(),
                            row.totalAmount()
                    ))
                    .toList();

            return new UserPageResponse<>(
                    items,
                    new CommonDTO.PageMeta(query.page(), query.size(), total)
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Không thể tải lịch sử đặt vé", exception);
            throw ApiException.internal("Không thể tải lịch sử đặt vé");
        }
    }

    private UserBookingCriteria validateAndCreateCriteria(UserBookingRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Yêu cầu lọc booking không hợp lệ");
        }

        String status = optionalValue(request.status(), "status");
        if (status != null) {
            status = status.toUpperCase();
            if (!STATUSES.contains(status)) {
                throw ApiException.badRequest("Trạng thái booking không hợp lệ");
            }
        }

        LocalDate from = parseDate(request.from(), "from");
        LocalDate to = parseDate(request.to(), "to");
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("from không được lớn hơn to");
        }

        int page = parseInteger(request.page(), DEFAULT_PAGE, "page");
        int size = parseInteger(request.size(), DEFAULT_SIZE, "size");
        if (page < 0) {
            throw ApiException.badRequest("page phải lớn hơn hoặc bằng 0");
        }
        if (size < 1 || size > 100) {
            throw ApiException.badRequest("size phải từ 1 đến 100");
        }

        String sort = request.sort() == null ? "createdAt,desc" : request.sort();
        UserBookingCriteria.SortDirection direction = switch (sort.toLowerCase()) {
            case "createdat,asc", "asc" -> UserBookingCriteria.SortDirection.ASC;
            case "createdat,desc", "desc" -> UserBookingCriteria.SortDirection.DESC;
            default -> UserBookingCriteria.SortDirection.DESC;
        };

        return new UserBookingCriteria(
                status,
                from == null ? null : from.atStartOfDay(APPLICATION_ZONE).toInstant(),
                to == null ? null : to.plusDays(1).atStartOfDay(APPLICATION_ZONE).toInstant(),
                page,
                size,
                direction
        );
    }

    private String optionalValue(String value, String name) {
        if (value == null) return null;
        if (value.isBlank()) {
            throw ApiException.badRequest(name + " không được để trống");
        }
        return value.trim();
    }

    private LocalDate parseDate(String value, String name) {
        String normalized = optionalValue(value, name);
        if (normalized == null) return null;
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeException exception) {
            throw ApiException.badRequest(name + " phải có định dạng yyyy-MM-dd");
        }
    }

    private int parseInteger(String value, int defaultValue, String name) {
        if (value == null) return defaultValue;
        if (value.isBlank()) throw ApiException.badRequest(name + " không được để trống");
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw ApiException.badRequest(name + " phải là số nguyên");
        }
    }
}
