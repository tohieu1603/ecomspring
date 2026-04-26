package com.hieu.catalog_service.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String name,
        String description,
        Long parentId,
        int sortOrder
) {}
