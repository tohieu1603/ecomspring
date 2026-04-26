package com.hieu.catalog_service.domain.model.category.valueobject;

/** Strongly-typed category identifier — may be {@code null} during factory creation
 *  (before the DB assigns a BIGSERIAL), but never after reconstitute. */
public record CategoryId(Long value) {

    public CategoryId {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException("CategoryId must be positive, got " + value);
        }
    }

    public static CategoryId of(Long value) {
        return new CategoryId(value);
    }
}
