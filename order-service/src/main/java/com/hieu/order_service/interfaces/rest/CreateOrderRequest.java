package com.hieu.order_service.interfaces.rest;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        List<ItemRequest> items,
        String recipientName,
        String recipientPhone,
        String street,
        String ward,
        String district,
        String city,
        String country,
        String postalCode,
        String paymentMethod,
        String notes,
        String voucherCode
) {
    public record ItemRequest(
            Long productId,
            String productName,
            Long variantId,
            String variantSku,
            String variantImage,
            BigDecimal unitPrice,
            int quantity
    ) {}
}
