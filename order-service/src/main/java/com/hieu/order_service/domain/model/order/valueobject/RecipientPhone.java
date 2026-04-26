package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

public record RecipientPhone(String value) {

    public RecipientPhone {
        Objects.requireNonNull(value, "RecipientPhone");
        if (value.isBlank()) throw new IllegalArgumentException("RecipientPhone must not be blank");
    }

    public static RecipientPhone of(String value) { return new RecipientPhone(value); }
}
