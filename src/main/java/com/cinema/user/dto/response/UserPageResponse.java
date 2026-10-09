package com.cinema.user.dto.response;

import com.cinema.common.dto.CommonDTO;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserPageResponse<T>(
        List<T> items,
        CommonDTO.PageMeta meta
) {
    public UserPageResponse(List<T> items) {
        this(items, null);
    }
}
