package com.cinema.user.service;

import com.cinema.common.dto.PageMeta;
import com.cinema.common.dto.PageResponse;
import com.cinema.user.dao.UserVoucherDAO;
import com.cinema.user.dto.response.UserVoucherResponse;

import java.util.List;

public class UserVoucherService {

    private final UserVoucherDAO voucherDAO = new UserVoucherDAO();

    // Danh sách voucher của người dùng
    public PageResponse<UserVoucherResponse> getUserVouchers(long userId, String status, int page, int size) {
        int p = Math.max(0, page);
        int s = (size < 1 || size > 100) ? 20 : size;

        String cleanStatus = null;
        if (status != null && !status.isBlank()) {
            cleanStatus = "AVAILABLE".equalsIgnoreCase(status.trim()) ? "UNUSED" : status.trim().toUpperCase();
        }

        long total = voucherDAO.count(userId, cleanStatus);
        List<UserVoucherResponse> items = voucherDAO.findPage(userId, cleanStatus, p, s).stream()
                .map(row -> new UserVoucherResponse(row.voucherId(), row.code(), row.status()))
                .toList();

        return PageResponse.of(items, new PageMeta(p, s, total));
    }
}
