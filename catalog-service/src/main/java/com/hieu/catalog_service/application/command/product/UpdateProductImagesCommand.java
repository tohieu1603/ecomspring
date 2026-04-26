package com.hieu.catalog_service.application.command.product;

import com.hieu.catalog_service.application.common.Command;
import com.hieu.catalog_service.application.dto.ProductDTO;

import java.util.List;

public record UpdateProductImagesCommand(
        Long productId,
        String thumbnail,
        List<String> images,
        String updatedBy
) implements Command<ProductDTO> {}
