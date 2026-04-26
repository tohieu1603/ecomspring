package com.hieu.order_service.domain.model.order.valueobject;

/** Positive item quantity. */
public record Quantity(int value) {

    public Quantity {
        if (value <= 0) throw new IllegalArgumentException("Quantity must be positive, got: " + value);
    }

    public static Quantity of(int value) { return new Quantity(value); }
}
