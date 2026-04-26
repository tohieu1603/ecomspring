package com.hieu.order_service.domain.model.order.valueobject;

import java.util.Objects;

public record ProductId(Long value) {

    public ProductId { Objects.requireNonNull(value, "ProductId value"); }

    public static ProductId of(Long value) { return new ProductId(value); }
}
