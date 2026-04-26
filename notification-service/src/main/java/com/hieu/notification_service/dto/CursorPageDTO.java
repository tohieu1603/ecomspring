package com.hieu.notification_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Cursor-based pagination envelope for infinite scroll. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CursorPageDTO<T>(
        List<T> items,
        Long nextCursor,
        int size,
        boolean hasNext
) {}
