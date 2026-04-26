package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;

public final class AttrAlreadyExistsException extends DomainException {
    public AttrAlreadyExistsException(String code) {
        super(CatalogErrorCodes.ATTR_ALREADY_EXISTS, "Attribute code already exists: " + code);
    }
}
