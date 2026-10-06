package com.cinema.user.criteria;

public record UserVoucherCriteria(
        String status,
        int page,
        int size
) {
    public long offset() {
        return (long) page * size;
    }
}
