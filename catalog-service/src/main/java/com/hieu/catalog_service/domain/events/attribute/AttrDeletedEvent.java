package com.hieu.catalog_service.domain.events.attribute;

import com.hieu.catalog_service.domain.events.DomainEvent;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class AttrDeletedEvent extends DomainEvent {

    private final Long attrId;

    public AttrDeletedEvent(Long attrId) {
        this.attrId = Objects.requireNonNull(attrId, "attrId");
    }

    @Override public String aggregateId() { return String.valueOf(attrId); }
}
