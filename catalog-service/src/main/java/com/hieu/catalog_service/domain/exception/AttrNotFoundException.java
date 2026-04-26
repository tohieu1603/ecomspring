package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;

public final class AttrNotFoundException extends DomainException {
    public AttrNotFoundException(Long attrId) {
        super(CatalogErrorCodes.ATTR_NOT_FOUND, "Attribute not found: " + attrId);
    }
    public AttrNotFoundException(String code) {
        super(CatalogErrorCodes.ATTR_NOT_FOUND, "Attribute not found: " + code);
    }
}
