package com.hieu.search_service.service;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.hieu.search_service.document.ProductDocument;
import com.hieu.search_service.dto.IndexProductRequest;
import com.hieu.search_service.dto.PageResponse;
import com.hieu.search_service.dto.SearchRequest;
import com.hieu.search_service.repository.ProductSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchApplicationService {

    // Allowlist prevents sort-field injection into ES field parameter
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdAt", "price", "totalStock", "name.keyword", "minPrice", "maxPrice");

    private final ProductSearchRepository repository;
    private final ElasticsearchOperations operations;

    /** Index (upsert) a single product document. */
    public void indexProduct(IndexProductRequest req) {
        var doc = ProductDocument.builder()
                .id(req.getId())
                .name(req.getName())
                .description(req.getDescription())
                .sku(req.getSku())
                .categoryId(req.getCategoryId())
                .categoryName(req.getCategoryName())
                .brand(req.getBrand())
                .price(req.getPrice())
                .minPrice(req.getMinPrice())
                .maxPrice(req.getMaxPrice())
                .totalStock(req.getTotalStock())
                .status(req.getStatus())
                .imageUrl(req.getImageUrl())
                .tags(req.getTags())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .build();
        repository.save(doc);
        log.debug("Indexed product id={}", req.getId());
    }

    /** Remove a product from the index. Runs asynchronously on the named pool — without
     *  the qualifier Spring falls back to SimpleAsyncTaskExecutor (unbounded thread
     *  creation, no monitoring) instead of the configured taskExecutor bean. */
    @Async("taskExecutor")
    public void removeProduct(String id) {
        repository.deleteById(id);
        log.debug("Removed product id={} from index", id);
    }

    /**
     * Partial update — only the {@code status} field. Used by status-changed events
     * which don't carry the full product payload, so a full save would clobber
     * existing name/brand/price with nulls.
     */
    public void updateStatus(String id, String newStatus) {
        repository.findById(id).ifPresentOrElse(
                doc -> {
                    doc.setStatus(newStatus);
                    repository.save(doc);
                    log.debug("Updated status id={} -> {}", id, newStatus);
                },
                () -> log.debug("status update: doc id={} not in index, skipping", id)
        );
    }

    /** Full-text + filtered search with pagination. */
    public PageResponse<ProductDocument> searchProducts(SearchRequest req) {
        var boolBuilder = new BoolQuery.Builder();

        // Full-text: multi_match across name^3, description, brand, categoryName
        if (StringUtils.hasText(req.getQ())) {
            boolBuilder.must(Query.of(q -> q.multiMatch(mm -> mm
                    .fields("name^3", "description", "brand", "categoryName")
                    .query(req.getQ()))));
        }

        // Term filters
        if (StringUtils.hasText(req.getStatus())) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("status").value(req.getStatus()))));
        }
        if (StringUtils.hasText(req.getCategoryId())) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("categoryId").value(req.getCategoryId()))));
        }
        if (StringUtils.hasText(req.getBrand())) {
            boolBuilder.filter(Query.of(q -> q.term(t -> t.field("brand").value(req.getBrand()))));
        }

        // Price range filter
        if (req.getMinPrice() != null || req.getMaxPrice() != null) {
            boolBuilder.filter(Query.of(q -> q.range(r -> {
                var rb = r.number(nr -> {
                    var nb = nr.field("minPrice");
                    if (req.getMinPrice() != null) nb.gte(req.getMinPrice());
                    if (req.getMaxPrice() != null) nb.lte(req.getMaxPrice());
                    return nb;
                });
                return rb;
            })));
        }

        String sortField = (req.getSortBy() != null && ALLOWED_SORT_FIELDS.contains(req.getSortBy()))
                ? req.getSortBy() : "createdAt";

        var nativeQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(boolBuilder.build())))
                .withPageable(PageRequest.of(req.getPage(), req.getSize()))
                .withSort(s -> s.field(f -> f.field(sortField).order(SortOrder.Desc)))
                .build();

        SearchHits<ProductDocument> hits = operations.search(nativeQuery, ProductDocument.class);
        List<ProductDocument> content = hits.getSearchHits().stream()
                .map(h -> h.getContent())
                .toList();

        int size = req.getSize();
        long total = hits.getTotalHits();
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        return PageResponse.<ProductDocument>builder()
                .content(content)
                .totalElements(total)
                .totalPages(totalPages)
                .page(req.getPage())
                .size(size)
                .build();
    }

    /** Prefix-based suggestions on product name. */
    public List<String> getSuggestions(String query, int size) {
        if (!StringUtils.hasText(query)) return List.of();

        var nativeQuery = NativeQuery.builder()
                .withQuery(Query.of(q -> q.prefix(p -> p.field("name").value(query.toLowerCase()))))
                .withPageable(PageRequest.of(0, size))
                .build();

        SearchHits<ProductDocument> hits = operations.search(nativeQuery, ProductDocument.class);
        return hits.getSearchHits().stream()
                .map(h -> h.getContent().getName())
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    /** Bulk upsert — used for full reindex. Returns count of indexed documents. */
    public int reindexAll(List<IndexProductRequest> requests) {
        List<ProductDocument> docs = new ArrayList<>(requests.size());
        for (IndexProductRequest req : requests) {
            docs.add(ProductDocument.builder()
                    .id(req.getId())
                    .name(req.getName())
                    .description(req.getDescription())
                    .sku(req.getSku())
                    .categoryId(req.getCategoryId())
                    .categoryName(req.getCategoryName())
                    .brand(req.getBrand())
                    .price(req.getPrice())
                    .minPrice(req.getMinPrice())
                    .maxPrice(req.getMaxPrice())
                    .totalStock(req.getTotalStock())
                    .status(req.getStatus())
                    .imageUrl(req.getImageUrl())
                    .tags(req.getTags())
                    .createdAt(req.getCreatedAt())
                    .updatedAt(req.getUpdatedAt())
                    .build());
        }
        repository.saveAll(docs);
        log.info("Bulk reindexed {} products", docs.size());
        return docs.size();
    }
}
