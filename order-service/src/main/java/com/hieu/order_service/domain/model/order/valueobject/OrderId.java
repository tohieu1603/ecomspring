package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

/** Surrogate PK wrapper. */
public record OrderId(Long value) {

    public OrderId { Objects.requireNonNull(value, "OrderId value"); }

    public static OrderId of(Long value) { return new OrderId(value); }
}
