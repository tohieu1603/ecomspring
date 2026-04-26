package com.hieu.catalog_service.application.dto;

public record AttrValDTO(
        Long id,
        Long attrId,
        String val,
        String code,
        int sortOrder
) {}
