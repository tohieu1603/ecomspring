package com.hieu.catalog_service.domain.events.category;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class CategoryDeletedEvent extends DomainEvent {

    private final Long categoryId;
    private final String deletedBy;

    public CategoryDeletedEvent(Long categoryId, String deletedBy) {
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.deletedBy = deletedBy;
    }

    @Override public String aggregateId() { return String.valueOf(categoryId); }
}
