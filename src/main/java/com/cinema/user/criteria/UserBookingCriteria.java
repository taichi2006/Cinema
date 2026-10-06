package com.cinema.user.criteria;

import java.time.Instant;

public record UserBookingCriteria(
        String status,
        Instant fromInclusive,
        Instant toExclusive,
        int page,
        int size,
        SortDirection sortDirection
) {
    public enum SortDirection {
        ASC,
        DESC
    }

    public long offset() {
        return (long) page * size;
    }
}
