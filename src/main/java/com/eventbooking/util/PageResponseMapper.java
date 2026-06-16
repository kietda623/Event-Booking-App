package com.eventbooking.util;

import com.eventbooking.dto.common.PageResponse;
import org.springframework.data.domain.Page;

public final class PageResponseMapper {
    private PageResponseMapper() {
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
