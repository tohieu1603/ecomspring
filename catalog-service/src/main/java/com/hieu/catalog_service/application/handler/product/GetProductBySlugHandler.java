package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.application.common.QueryHandler;
import com.hieu.catalog_service.application.dto.ProductDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.application.query.product.GetProductBySlugQuery;
import com.hieu.catalog_service.domain.exception.ProductNotFoundException;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.model.product.valueobject.Slug;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProductBySlugHandler implements QueryHandler<GetProductBySlugQuery, ProductDTO> {

    private final ProductRepository productRepository;
    private final CatalogDtoMapper mapper;

    @Override
    public ProductDTO handle(GetProductBySlugQuery query) {
        var product = productRepository.findBySlug(Slug.of(query.slug()))
            .orElseThrow(() -> new ProductNotFoundException("slug=" + query.slug()));
        return productRepository.findByIdWithVariants(ProductId.of(product.getId().value()))
            .map(mapper::toDto)
            .orElseThrow(() -> new ProductNotFoundException(product.getId().value()));
    }
}
