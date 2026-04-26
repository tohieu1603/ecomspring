package com.hieu.catalog_service.application.command.variant;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.VariantDTO;

/** Delta-adjust stock — negative value deducts, positive receives. */
public record AdjustVariantStockCommand(
        Long productId,
        Long variantId,
        int delta,
        String updatedBy
) implements Command<VariantDTO> {}
