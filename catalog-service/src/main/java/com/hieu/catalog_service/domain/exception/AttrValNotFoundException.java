package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;

public final class AttrValNotFoundException extends DomainException {
    public AttrValNotFoundException(Long valId) {
        super(CatalogErrorCodes.ATTR_VAL_NOT_FOUND, "Attribute value not found: " + valId);
    }
}
