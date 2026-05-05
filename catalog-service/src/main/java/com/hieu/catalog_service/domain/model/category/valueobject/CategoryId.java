package com.hieu.catalog_service.domain.model.category.valueobject;

import java.util.Objects;

/**
 * Strongly-typed category identifier backed by BIGSERIAL.
 *
 * <p>Aggregate factory creates {@code Category} with {@code id=null} (DB assigns later
 * via {@code assignId()}). The VO itself rejects null — null id fields bypass this VO,
 * but any code that explicitly wraps an id must provide a real value.
 */
public record CategoryId(Long value) {

    public CategoryId {
        Objects.requireNonNull(value, "CategoryId cannot be null");
        if (value <= 0) {
            throw new IllegalArgumentException("CategoryId must be positive, got " + value);
        }
    }

    public static CategoryId of(Long value) {
        return new CategoryId(value);
    }
}
