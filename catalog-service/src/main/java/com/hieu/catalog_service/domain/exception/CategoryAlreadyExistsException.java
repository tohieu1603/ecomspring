package com.hieu.catalog_service.domain.exception;

import com.hieu.catalog_service.domain.shared.DomainException;

public final class CategoryAlreadyExistsException extends DomainException {
    public CategoryAlreadyExistsException(String name) {
        super(CatalogErrorCodes.CATEGORY_ALREADY_EXISTS, "Category name already exists: " + name);
    }
}
