package com.hieu.catalog_service.domain.model.product.valueobject;

import java.util.Objects;

/** Strongly-typed variant (SKU row) identifier. See {@link ProductId} for rationale. */
public record VariantId(Long value) {

    public VariantId {
        Objects.requireNonNull(value, "VariantId cannot be null");
        if (value <= 0) {
            throw new IllegalArgumentException("VariantId must be positive, got " + value);
        }
    }

    public static VariantId of(Long value) {
        return new VariantId(value);
    }
}
