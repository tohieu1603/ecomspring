package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;

public final class VariantSkuAlreadyExistsException extends DomainException {
    public VariantSkuAlreadyExistsException(String sku) {
        super(CatalogErrorCodes.VARIANT_SKU_ALREADY_EXISTS, "SKU already exists: " + sku);
    }
}
