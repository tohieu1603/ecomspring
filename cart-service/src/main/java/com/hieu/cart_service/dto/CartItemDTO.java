package com.hieu.cart_service.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Immutable view of a single cart line item. */
public record CartItemDTO(
        Long id,
        Long productId,
        String productName,
        Long variantId,
        String variantSku,
        String variantImage,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        String warning,
        Instant updatedAt
) {}
