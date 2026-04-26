package com.hieu.catalog_service.application.query.variant;

import com.hieu.catalog_service.application.common.Query;

/** {@code true} if the SKU is ACTIVE and stock &gt;= {@code requested}. */
public record CheckStockQuery(String sku, int requested) implements Query<Boolean> {}
