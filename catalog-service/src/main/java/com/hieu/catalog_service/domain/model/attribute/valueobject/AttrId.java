package com.hieu.catalog_service.domain.model.attribute.valueobject;

/** Strongly-typed identifier for an {@code attrs} row (an attribute definition). */
public record AttrId(Long value) {

    public AttrId {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException("AttrId must be positive, got " + value);
        }
    }

    public static AttrId of(Long value) {
        return new AttrId(value);
    }
}
