package com.cinema.user.service;

import com.cinema.common.exception.ApiException;
import com.cinema.user.criteria.UserVoucherCriteria;
import com.cinema.user.dao.UserVoucherDAO;
import com.cinema.user.dto.request.UserVoucherRequest;
import com.cinema.user.dto.response.UserPageResponse;
import com.cinema.user.dto.response.UserVoucherResponse;

import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserVoucherService {

    private static final Logger LOGGER = Logger.getLogger(UserVoucherService.class.getName());
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final Set<String> STATUSES = Set.of("UNUSED", "USED", "EXPIRED");

    private final UserVoucherDAO voucherDAO;

    public UserVoucherService() {
        this(new UserVoucherDAO());
    }

    public UserVoucherService(UserVoucherDAO voucherDAO) {
        this.voucherDAO = Objects.requireNonNull(voucherDAO, "UserVoucherDAO must not be null");
    }

    public UserPageResponse<UserVoucherResponse> getUserVouchers(
            long userId,
            UserVoucherRequest request
    ) {
        UserVoucherCriteria query = validateAndCreateCriteria(request);
        try {
            var items = voucherDAO.findPage(userId, query).stream()
                    .map(row -> new UserVoucherResponse(
                            row.voucherId(),
                            row.code(),
                            row.status()
                    ))
                    .toList();

            return new UserPageResponse<>(items);
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Không thể tải danh sách voucher", exception);
            throw ApiException.internal("Không thể tải danh sách voucher");
        }
    }

    private UserVoucherCriteria validateAndCreateCriteria(UserVoucherRequest request) {
        if (request == null) {
            return new UserVoucherCriteria(null, DEFAULT_PAGE, DEFAULT_SIZE);
        }

        String status = optionalValue(request.status(), "status");
        if (status != null) {
            status = status.toUpperCase();
            if ("AVAILABLE".equals(status)) {
                status = "UNUSED";
            }
            if (!STATUSES.contains(status)) {
                throw ApiException.badRequest("Trạng thái voucher không hợp lệ");
            }
        }

        int page = parseInteger(request.page(), DEFAULT_PAGE, "page");
        int size = parseInteger(request.size(), DEFAULT_SIZE, "size");
        if (page < 0) {
            throw ApiException.badRequest("page phải lớn hơn hoặc bằng 0");
        }
        if (size < 1 || size > 100) {
            throw ApiException.badRequest("size phải từ 1 đến 100");
        }

        return new UserVoucherCriteria(status, page, size);
    }

    private String optionalValue(String value, String name) {
        if (value == null) return null;
        if (value.isBlank()) {
            throw ApiException.badRequest(name + " không được để trống");
        }
        return value.trim();
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
