package com.hieu.order_service.domain.model.order.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record RefundAmount(BigDecimal amount) {

    public RefundAmount {
        Objects.requireNonNull(amount, "RefundAmount");
        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("RefundAmount cannot be negative");
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static RefundAmount of(BigDecimal value) { return value == null ? null : new RefundAmount(value); }
    public static RefundAmount of(String value) { return value == null ? null : new RefundAmount(new BigDecimal(value)); }
}
