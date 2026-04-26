package com.hieu.catalog_service.application.dto;

public record VariantAttrDTO(
        Long id,
        Long attrId,
        String attrCode,
        String attrName,
        Long valId,
        String valText
) {}
