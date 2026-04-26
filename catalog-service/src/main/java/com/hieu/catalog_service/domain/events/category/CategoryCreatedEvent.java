package com.hieu.catalog_service.domain.events.category;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class CategoryCreatedEvent extends DomainEvent {

    private final Long categoryId;
    private final String name;
    private final Long parentId;
    private final String createdBy;

    public CategoryCreatedEvent(Long categoryId, String name, Long parentId, String createdBy) {
        this.categoryId = Objects.requireNonNull(categoryId, "categoryId");
        this.name = Objects.requireNonNull(name, "name");
        this.parentId = parentId;
        this.createdBy = createdBy;
    }

    @Override public String aggregateId() { return String.valueOf(categoryId); }
}
