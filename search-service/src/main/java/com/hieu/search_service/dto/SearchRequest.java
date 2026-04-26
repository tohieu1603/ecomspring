package com.hieu.search_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    private String q;
    private String brand;
    private String categoryId;
    private Double minPrice;
    private Double maxPrice;
    /** ACTIVE, INACTIVE, etc. */
    private String status;
    /** field name to sort by, default "createdAt" */
    private String sortBy;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
}
