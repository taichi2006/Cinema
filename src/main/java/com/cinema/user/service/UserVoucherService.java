package com.cinema.user.service;

import com.cinema.common.exception.ApiException;
import com.cinema.user.criteria.UserVoucherCriteria;
import com.cinema.user.dao.UserVoucherDAO;
import com.cinema.user.dto.response.UserVoucherResponse;
import com.cinema.common.dto.PageResponse;
import com.cinema.common.dto.PageMeta;
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

    public PageResponse<UserVoucherResponse> getUserVouchers(
            long userId,
            UserVoucherCriteria query) {
        try {
            long total = voucherDAO.count(userId, query);
            var items = voucherDAO.findPage(userId, query).stream()
                    .map(row -> new UserVoucherResponse(
                            row.voucherId(),
                            row.code(),
                            row.status()))
                    .toList();

            return new PageResponse<>(items, new PageMeta(query.page(), query.size(), total));
        } catch (ApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Không thể tải danh sách voucher", exception);
            throw ApiException.internal("Không thể tải danh sách voucher");
        }
    }


}
