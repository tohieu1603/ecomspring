package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;

/** Thrown when a product operation violates a lifecycle invariant (e.g. editing a deleted product). */
public final class InvalidProductStateException extends DomainException {
    public InvalidProductStateException(String message) {
        super(CatalogErrorCodes.PRODUCT_INVALID_STATE, message);
    }
}
