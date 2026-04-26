package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

public record IdempotencyKey(String value) {

    public IdempotencyKey { Objects.requireNonNull(value, "IdempotencyKey value"); }

    public static IdempotencyKey of(String value) {
        return value == null || value.isBlank() ? null : new IdempotencyKey(value.trim());
    }
}
