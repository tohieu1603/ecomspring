package com.hieu.order_service.application.dto;

import java.math.BigDecimal;

public record OrderItemDTO(
        Long id,
        Long productId,
        String productName,
        Long variantId,
        String variantSku,
        String variantImage,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {}
