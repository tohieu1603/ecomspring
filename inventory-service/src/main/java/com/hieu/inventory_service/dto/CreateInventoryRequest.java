package com.hieu.inventory_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request to create a new inventory record. */
public record CreateInventoryRequest(
    @NotNull Long productId,
    @NotBlank String sku,
    @NotNull @Min(0) Integer quantity,
    Integer minStockLevel
) {}
