package com.hieu.catalog_service.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Lightweight projection for list endpoints — no variants array to keep payload small. */
public record ProductSummaryDTO(
        Long id,
        String name,
        String slug,
        Long categoryId,
        String brand,
        String thumbnail,
        String status,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int totalStock,
        boolean available,
        Instant createdAt
) {}
