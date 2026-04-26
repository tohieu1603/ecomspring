package com.hieu.catalog_service.domain.exception;

/**
 * Stable error-code catalogue for catalog-service domain failures.
 *
 * <p>Codes use the {@code CATALOG-NNNN} format documented on
 * {@link com.hieu.catalog_service.domain.shared.DomainException} and must never be
 * renamed — only appended. The REST {@code GlobalExceptionHandler} maps each family to
 * an HTTP status so clients can branch on the code without parsing English messages.
 */
public final class CatalogErrorCodes {

    private CatalogErrorCodes() {}

    // Product (2000-2099)
    public static final String PRODUCT_NOT_FOUND       = "CATALOG-2001";
    public static final String PRODUCT_ALREADY_EXISTS  = "CATALOG-2002";
    public static final String PRODUCT_INVALID_STATE   = "CATALOG-2003";

    // Variant (2100-2199)
    public static final String VARIANT_NOT_FOUND       = "CATALOG-2101";
    public static final String VARIANT_SKU_ALREADY_EXISTS = "CATALOG-2102";
    public static final String VARIANT_INVALID         = "CATALOG-2103";

    // Category (2200-2299)
    public static final String CATEGORY_NOT_FOUND      = "CATALOG-2201";
    public static final String CATEGORY_ALREADY_EXISTS = "CATALOG-2202";
    public static final String CATEGORY_CYCLE          = "CATALOG-2203";

    // Attribute (2300-2399)
    public static final String ATTR_NOT_FOUND          = "CATALOG-2301";
    public static final String ATTR_ALREADY_EXISTS     = "CATALOG-2302";
    public static final String ATTR_VAL_NOT_FOUND      = "CATALOG-2303";
    public static final String ATTR_INVALID_TYPE       = "CATALOG-2304";
}
