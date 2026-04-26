package com.hieu.catalog_service.application.handler.product;

import com.hieu.catalog_service.application.common.CursorCodec;
import com.hieu.catalog_service.application.common.QueryHandler;
import com.hieu.catalog_service.application.dto.PageDTO;
import com.hieu.catalog_service.application.dto.ProductSummaryDTO;
import com.hieu.catalog_service.application.mapper.CatalogDtoMapper;
import com.hieu.catalog_service.application.query.product.ListProductsQuery;
import com.hieu.catalog_service.domain.model.product.Product;
import com.hieu.catalog_service.domain.model.product.valueobject.ProductId;
import com.hieu.catalog_service.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cursor-paginated product listing. Keyset pagination using {@code (createdAt, id)} avoids
 * OFFSET's O(N) scan cost on deep pages. Over-fetches one row to detect {@code hasNext}
 * without a count query.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListProductsHandler implements QueryHandler<ListProductsQuery, PageDTO<ProductSummaryDTO>> {

    private static final int MAX_LIMIT = 100;

    private final ProductRepository productRepository;
    private final CatalogDtoMapper mapper;

    @Override
    public PageDTO<ProductSummaryDTO> handle(ListProductsQuery query) {
        int pageSize = Math.clamp(query.limit(), 1, MAX_LIMIT);
        var cursor = CursorCodec.decode(query.cursor());

        List<ProductId> ids = cursor == null
            ? productRepository.findFirstPageIds(pageSize + 1)
            : productRepository.findIdsAfterCursor(cursor.createdAt(), cursor.id(), pageSize + 1);

        boolean hasNext = ids.size() > pageSize;
        List<ProductId> pageIds = hasNext ? ids.subList(0, pageSize) : ids;

        List<Product> products = productRepository.findAllByIdsWithVariants(pageIds);
        List<ProductSummaryDTO> items = products.stream().map(mapper::toSummary).toList();

        String next = null;
        if (hasNext && !products.isEmpty()) {
            Product last = products.getLast();
            next = CursorCodec.encode(last.getCreatedAt(), last.getId().value());
        }
        return PageDTO.of(items, next, pageSize, -1);
    }
}
