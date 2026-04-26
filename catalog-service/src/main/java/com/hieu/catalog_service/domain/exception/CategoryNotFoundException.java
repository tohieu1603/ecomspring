package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;

public final class CategoryNotFoundException extends DomainException {
    public CategoryNotFoundException(Long categoryId) {
        super(CatalogErrorCodes.CATEGORY_NOT_FOUND, "Category not found: " + categoryId);
    }
}
