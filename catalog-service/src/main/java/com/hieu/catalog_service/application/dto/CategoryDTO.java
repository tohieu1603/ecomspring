package com.hieu.catalog_service.application.dto;

import java.time.Instant;

public record CategoryDTO(
        Long id,
        String name,
        String description,
        Long parentId,
        boolean active,
        int sortOrder,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String updatedBy
) {}
