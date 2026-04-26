package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

/** Human-readable order reference: {@code ORD-yyyyMMdd-NNNNNN}. */
public record OrderNumber(String value) {

    public OrderNumber {
        Objects.requireNonNull(value, "OrderNumber value");
        if (value.isBlank()) throw new IllegalArgumentException("OrderNumber must not be blank");
    }

    public static OrderNumber of(String value) { return new OrderNumber(value); }
}
