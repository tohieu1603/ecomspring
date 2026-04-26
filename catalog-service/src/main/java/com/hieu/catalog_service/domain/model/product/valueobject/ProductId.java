package com.hieu.catalog_service.domain.model.product.valueobject;

import java.util.Objects;

/**
 * Strongly-typed product identifier backed by a BIGSERIAL on the persistence side.
 *
 * <p>Using a dedicated VO instead of a raw {@code Long} prevents accidental mixing of
 * {@link ProductId} / {@link VariantId} / {@link com.hieu.catalog_service.domain.model.category.valueobject.CategoryId}
 * at method signatures — the compiler catches it instead of a runtime bug.
 */
public record ProductId(Long value) {

    public ProductId {
        Objects.requireNonNull(value, "ProductId cannot be null");
        if (value <= 0) {
            throw new IllegalArgumentException("ProductId must be positive, got " + value);
        }
    }

    public static ProductId of(Long value) {
        return new ProductId(value);
    }
}
