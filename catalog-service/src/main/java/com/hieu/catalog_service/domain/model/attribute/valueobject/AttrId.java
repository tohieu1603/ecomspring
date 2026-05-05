package com.hieu.catalog_service.domain.model.attribute.valueobject;

import java.util.Objects;

/**
 * Strongly-typed identifier for an {@code attrs} row (an attribute definition).
 *
 * <p>BIGSERIAL — aggregate factory creates {@code Attr} with id=null (DB assigns later).
 * VO itself rejects null for consistency with sibling Id VOs.
 */
public record AttrId(Long value) {

    public AttrId {
        Objects.requireNonNull(value, "AttrId cannot be null");
        if (value <= 0) {
            throw new IllegalArgumentException("AttrId must be positive, got " + value);
        }
    }

    public static AttrId of(Long value) {
        return new AttrId(value);
    }
}
