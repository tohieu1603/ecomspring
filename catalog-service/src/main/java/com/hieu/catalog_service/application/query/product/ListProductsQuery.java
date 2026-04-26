package com.hieu.catalog_service.application.query.product;

import com.hieu.catalog_service.application.common.Query;
import com.hieu.catalog_service.application.dto.PageDTO;
import com.hieu.catalog_service.application.dto.ProductSummaryDTO;

public record ListProductsQuery(String cursor, int limit) implements Query<PageDTO<ProductSummaryDTO>> {}
