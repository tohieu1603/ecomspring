package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;

public final class ProductNotFoundException extends DomainException {
    public ProductNotFoundException(Long productId) {
        super(CatalogErrorCodes.PRODUCT_NOT_FOUND, "Product not found: " + productId);
    }

    public ProductNotFoundException(String criterion) {
        super(CatalogErrorCodes.PRODUCT_NOT_FOUND, "Product not found: " + criterion);
    }
}
