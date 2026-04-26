package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;

public final class ProductAlreadyExistsException extends DomainException {
    public ProductAlreadyExistsException(String slug) {
        super(CatalogErrorCodes.PRODUCT_ALREADY_EXISTS, "Product slug already in use: " + slug);
    }
}
