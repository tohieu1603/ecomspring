package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;

public final class VariantNotFoundException extends DomainException {
    public VariantNotFoundException(Long variantId) {
        super(CatalogErrorCodes.VARIANT_NOT_FOUND, "Variant not found: " + variantId);
    }

    public static VariantNotFoundException bySku(String sku) {
        return new VariantNotFoundException(sku, true);
    }

    private VariantNotFoundException(String sku, boolean bySku) {
        super(CatalogErrorCodes.VARIANT_NOT_FOUND, "Variant not found by SKU: " + sku);
    }
}
