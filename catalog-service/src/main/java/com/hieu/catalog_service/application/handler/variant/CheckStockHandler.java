package com.hieu.catalog_service.application.handler.variant;

import com.hieu.catalog_service.application.common.QueryHandler;
import com.hieu.catalog_service.application.query.variant.CheckStockQuery;
import com.hieu.catalog_service.domain.model.product.valueobject.Sku;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckStockHandler implements QueryHandler<CheckStockQuery, Boolean> {

    private final ProductRepository productRepository;

    @Override
    public Boolean handle(CheckStockQuery query) {
        var sku = Sku.of(query.sku());
        return productRepository.findBySku(sku)
            .flatMap(p -> p.getVariants().stream().filter(v -> v.getSku().equals(sku)).findFirst())
            .map(v -> v.getStatus().canSell() && v.getQuantity().value() >= query.requested())
            .orElse(false);
    }
}
