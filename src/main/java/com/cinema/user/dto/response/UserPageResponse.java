package com.cinema.user.dto.response;

import com.cinema.common.dto.CommonDTO;

import java.util.List;

public record UserPageResponse<T>(
        List<T> items,
        CommonDTO.PageMeta meta
) {
}
