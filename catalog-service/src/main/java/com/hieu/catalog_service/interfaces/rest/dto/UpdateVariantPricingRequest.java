package com.hieu.catalog_service.interfaces.rest.dto;

import java.math.BigDecimal;

public record UpdateVariantPricingRequest(BigDecimal price, BigDecimal cost, BigDecimal salePrice) {}
