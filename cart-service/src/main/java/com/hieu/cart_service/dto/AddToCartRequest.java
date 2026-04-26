package com.hieu.cart_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Request body for POST /api/cart/items. */
public record AddToCartRequest(
        @NotNull Long productId,
        @NotNull Long variantId,
        @NotNull @Min(1) @Max(999) Integer quantity,
        String idempotencyKey
) {}
