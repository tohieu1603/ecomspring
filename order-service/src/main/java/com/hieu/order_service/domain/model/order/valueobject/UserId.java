package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

/** User UUID string wrapper. */
public record UserId(String value) {

    public UserId {
        Objects.requireNonNull(value, "UserId value");
        if (value.isBlank()) throw new IllegalArgumentException("UserId must not be blank");
    }

    public static UserId of(String value) { return new UserId(value); }
}
